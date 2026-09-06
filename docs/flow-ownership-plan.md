# План: владение ресурсами через cold flow

Документ для передачи работы между сессиями. Здесь план, принятые решения и текущий статус каждого пункта.
Обновляется после каждого шага; коммиты в стиле репозитория (`[REFACTOR]`, `[TEST]`, `[DOCS]`, `[FIX]`), без
`Co-Authored-By`. Перед началом работы прочитать `AGENTS.md` и `.claude/docs/*`.

## Принцип

Любой ресурс, который надо освобождать (Gradle-ран, подписка на message bus, `ProcessListener`, JCEF load handler),
представляется как cold `Flow`: подписка захватывает ресурс, отмена подписчика освобождает его в `awaitClose` или в
`finally { withContext(NonCancellable) { ... } }`. Замена «последний запрос побеждает» делается через
`flatMapLatest`/`mapLatest`, а не через ручные `Job`-поля, generation-счётчики и `Mutex`. Горячее состояние наружу
отдаётся через `stateIn` на scope владельца, поэтому `state.first { ... }` безопасен и не трогает ресурс.

Образец уже в коде: `components/server/api/.../DevServerLauncher.kt` (cold flow одного запуска) и
`DefaultDevServerController.kt` (`request.flatMapLatest { launcher.launch(it.host) }.stateIn(...)`),
коммиты `bf4e88c`, `d5e6aca`, `b1d709c`.

## Пункты

### 1. `GradleTaskRunner.run(): Flow<GradleRunEvent>` вместо коллбэков — СДЕЛАНО

Проблема. У одного рана три ресурса с разным временем жизни: `Disposer.newDisposable(...)` без родителя в
`ExternalSystemGradleTaskRunner.subscribeToOutput` (утекает, если `runTask` бросит до старта), `ProcessListener`,
который `RunOutputForwarder` вешает и никогда не снимает, и мост из коллбэков в корутину
(`DevServerProcessListener`, `SilentProcessListener`, `DevServerRunListener` с лямбдами, `TaskCallback`-адаптер).

Решение.
- `GradleTaskRunner.run(config, executionName): Flow<GradleRunEvent>` — cold. Подписка отправляет задачу в платформу,
  события: `Started` (задача передана платформе), `Output(text)`, `Exited(isSuccess)`; после `Exited` flow завершается.
  Отмена подписчика только **отписывается** от вывода (message bus, `removeProcessListener`), ран не трогает.
  Останавливает ран по-прежнему явный `stop(executionName)`. Так стоп-таска Kobweb может быть запущена и брошена:
  `run(stopConfig, STOP_TASK).firstOrNull()` возвращается на `Started`.
- В `ExternalSystemGradleTaskRunner` всё это один `callbackFlow`: `Disposable` для подписки создаётся в теле и
  освобождается в `awaitClose`, там же снимается `ProcessListener` с найденного handler'а.
- `DevServerLauncher` собирает `run(...)` дочерней корутиной внутри своего `channelFlow` и складывает факты в
  `DevServerRunSignals` (два `CompletableDeferred`): это единственный оставшийся мост, и он уже внутри владельца.
- `DevServerRunListener` (буферизация строк + поиск URL) становится `AnnouncedBaseUrlParser` без коллбэков:
  `feed(chunk): String?` возвращает origin один раз.
- Удаляются `DevServerProcessListener`, `SilentProcessListener`, `RunOutputForwarder`.
- Фейк: `FakeGradleTaskRunner.run` регистрирует `StartedRun` при подписке и отдаёт `Channel` событий; тесты пишут
  в него через `printOutput(text)` / `exit(isSuccess)`.

Открытый вопрос (не решён в рамках пункта): исключение из `runTask` при запуске стоп-таски в `finally` лаунчера
пробросится наружу и убьёт `stateIn`-цепочку. Сегодня поведение такое же; логгера в `api`-модулях нет.

### 2. `EditorTracker` на `callbackFlow` + `mapLatest` — СДЕЛАНО

Проблема. `events` это `Channel(UNLIMITED)`, который никогда не закрывается. Если `scan` бросит не-cancellation
исключение, `coroutineScope` в `trackEvents` умирает, а слушатели остаются на `listenerDisposable` и продолжают
`trySend` в канал без читателя: трекер молча мёртв до закрытия проекта, память растёт на каждое нажатие.
Два владельца у одного ресурса (слушатели по `Disposable`, потребитель по scope) плюс ручной `mapLatest`
(`scanJob`, `currentFile`, `restartScan` с `cancelAndJoin`).

Решение.
- `selectedFiles(): Flow<VirtualFile?>` и `edits(): Flow<VirtualFile>` как `callbackFlow` c `awaitClose`,
  отписывающим от `FILE_EDITOR_MANAGER` и `EditorEventMulticaster`. Начальный выбор читается в теле flow на
  `mainContext` вместе с подпиской, чтобы не проскочило событие.
- Цепочка:
  `selectedFiles().flatMapLatest { file -> if (file == null) flowOf(null) else edits().filter { it == file }.debounce(RESCAN_DEBOUNCE).map { file }.onStart { emit(file) } }.mapLatest { file -> file?.let { scan(it) } }.collect(contract::onFileSelected)`.
  `mapLatest` отменяет незавершённый скан при новом событии и сохраняет порядок результатов.
- `start(parentDisposable)` заменяется на `Job`, который `IntellijPreviewModule.lifecycle` запускает в `onEnable` и
  отменяет в `onDisable`. `EditorEvent` и канал удаляются.
- Тесты `EditorTracker` сейчас отсутствуют (intellij-модуль); проверка через `buildPlugin` + smoke в sandbox
  (см. `WORKLOG.md`, раздел про Robot).

### 3. `PreviewFeature.connectIfNeeded` через `mapLatest` — СДЕЛАНО

Проблема. Каждое событие редактора делает `launch { resolveHost(); requestRunning() }`; быстрое переключение файлов
даёт параллельные `readAction`, отменить устаревший `locate` нечем. Состояние не портится (редьюсер сверяет
`filePath`), дублируется только работа. Также `launch { toolWindowPresenter.show() }` в `onFocusPreview`.

Решение. `MutableStateFlow<ConnectRequest?>` (target + retryAfterFailure + attempt) и в `init`
`requests.filterNotNull().mapLatest { resolveHost(); requestRunning() }.launchIn(this)`. Тесты `PreviewFeatureTest`
уже покрывают контракт; добавить тест «второй запрос отменяет первый resolve».

### 4. `PreviewPanel`: загрузки страницы как flow — СДЕЛАНО

`PreviewBrowser.pageLoads(): Flow<String>` как `callbackFlow` с `awaitClose { removeLoadHandler }` вместо
`addPageLoadListener`; панель делает `combine(contract.state, browser.pageLoads())` с одним `render`, уходит
`launch {}` для прыжка на EDT и часть мутабельных полей. Утечки нет, JCEF умирает вместе с окном; это стиль.

### 5. Мелочи — СДЕЛАНО

- `HttpDevServerHealthCheck.isAlive`: `runCatching` ловит `CancellationException`; заменить на `catch (IOException)`.
- `ToolWindowVisibilityTracker`, `SourceChangeTracker`: перевести на `callbackFlow` только ради единообразия
  после пункта 2, чтобы `IntellijPreviewModule.lifecycle` стал одним `Job`.

Не трогать: `GradleModuleCatalog`, `ModuleDependencyGraphReader`, `IntellijPreviewHostLocator`, `PreviewFileScanner`,
`KobwebConfReader`, actions, line marker — stateless запросы, flow им не нужен.

### 6. Закрытие проекта: остановка без Gradle и без диалога — СДЕЛАНО

Найдено smoke-тестом в sandbox после пунктов 1–5: проект закрыт, а Kobweb-сервер на 8086 остался жить.
Причины. (а) Платформа при закрытии проекта первой находит наш continuous-ран `kobwebStart -t` и показывает
модальный диалог «Process ... Is Running»; это происходит в `canClose`, раньше отмены нашего scope. (б) `finally`
лаунчера запускал `kobwebStop` как Gradle-таску через `ExternalSystemUtil.runTask(project)`, а проект уже
закрывается: таска не выполняется.

Решение.
- `KobwebServerStopper` (`server:api`, чистый JDK) повторяет `kobwebStop`: читает pid из
  `<module>/.kobweb/server/state.yaml`, `ProcessHandle.destroy()`, ждёт до 10 с, затем `destroyForcibly()`.
  Порт `DetachedServerStopper`; `DevServerKind.stopTask` заменён на `isServerDetached`. `DevServerRunNames.STOP_TASK`
  и стоп-таска через Gradle удалены. В `stopRun` сервер гасится первым: ему не нужен проект.
- `RunOutputForwarder.processStarted` ставит `ProcessHandler.SILENTLY_DESTROY_ON_CLOSE`: платформа убивает наш ран
  при закрытии молча, без диалога.
- `ExternalSystemGradleTaskRunner.stop` выходит сразу, если `project.isDisposed`: ранов уже нет.
- Лаунчер после выхода рана (любого, в том числе убитого платформой) сначала проверяет, отвечает ли сервер, и если да,
  остаётся владельцем (`Running` + `awaitCancellation`), а не выходит из flow как «ничего не осталось». Без этого
  Kobweb-сервер терялся: платформа убивает ран до отмены scope, лаунчер получал `Exited(false)` и завершался.
- Тест `KobwebServerStopperTest` на реальном процессе (`sleep 60`) и `runBlocking`; тест лаунчера
  «run killed but server answers → still owned → stopped on cancel».
- `ExternalSystemGradleTaskRunner` пишет INFO в idea.log: исход рана, остановка, пропуск при disposed проекте.
- Проверено smoke в sandbox: после `closeAndDispose` ран убит платформой молча (без диалога), сервер остаётся
  `Running` под владением, при dispose stopper гасит его, порт 8086 перестаёт отвечать через 2 с.

Ограничение: при выходе из IDE `finally` выполняется асинхронно после отмены scope; `destroy()` отправляется
немедленно, но гарантии до завершения JVM нет.

## Проверка каждого шага

```
./gradlew detekt :components:server:api:test :components:preview:api:test :plugin:compileKotlin
```

Полный прогон перед финалом: `./gradlew detekt test :plugin:buildPlugin`.

## Журнал

- 2026-09-06: аудит проведён, план записан.
- 2026-09-06: пункт 1 сделан. `GradleTaskRunner.run()` cold flow, `ExternalSystemGradleTaskRunner` на `callbackFlow`
  с `try/finally` (подписка и `ProcessListener` освобождаются вместе, в том числе если `runTask` бросил),
  `RunOutputForwarder.detach()` с защитой от гонки со стартом процесса, `AnnouncedBaseUrlParser` вместо
  `DevServerRunListener`, удалены `DevServerProcessListener`, `SilentProcessListener`. Тесты server:api: 44, все зелёные.
- 2026-09-06: пункт 2 сделан. Политика «что и когда сканировать» вынесена в чистый `ScanScheduler` (`preview:api`,
  `source/`), 7 тестов на виртуальном времени. `EditorTracker` держит два `callbackFlow` (`selectedFiles`,
  `editedFiles`) с `Disposer.newDisposable` в теле и `awaitClose { dispose }`; `track()` это suspend до отмены,
  `mapLatest` вместо `scanJob`/`cancelAndJoin`. В `core:api` добавлен `CoroutineLifecycle(scope, block)`: запускает
  в `onEnable`, отменяет в `onDisable`; `IntellijPreviewModule.lifecycle` стал `CompositeLifecycle`. `EditorEvent` и
  канал удалены. 
- 2026-09-06: пункт 3 сделан. `PreviewConnectRequest(target, retryAfterFailure, attempt)` в `MutableStateFlow`,
  в `init` `connectRequests.filterNotNull().mapLatest(::connect).launchIn(this)`. `FakePreviewHostLocator.locateDelay`
  позволяет прервать lookup новым запросом; тест «stale lookup dropped» добавлен, PreviewFeatureTest: 18.
  `launch { toolWindowPresenter.show() }` в `onFocusPreview` оставлен: это прыжок на EDT, не ресурс.
  Следующий: пункт 5 (мелочи), затем 4 по желанию.
- 2026-09-06: пункт 5 сделан. `HttpDevServerHealthCheck` ловит только `IOException`. `SourceChangeTracker` и
  `ToolWindowVisibilityTracker` отдают `callbackFlow` и `suspend fun track()`, как `EditorTracker`.
  `IntellijPreviewModule.lifecycle` это один `CoroutineLifecycle` с `supervisorScope { launch × 3 }`.
  `IntellijCoreModule.listenerDisposable` и его `lifecycle` удалены, `RootModule` и `PreviewProjectService` без
  `parentDisposable`; `RootModule.lifecycle` = `intellijPreviewModule.lifecycle`. Следующий: пункт 4.
- 2026-09-06: пункт 4 сделан. `PreviewBrowser.pageLoads(): Flow<String>` вместо `addPageLoadListener`;
  `JcefPreviewBrowser` снимает `CefLoadHandler` в `awaitClose` (с проверкой `isDisposed`: tool window может
  освободить браузер раньше отмены scope), `UnsupportedPreviewBrowser` отдаёт `MutableSharedFlow`. Панель собирает
  загрузки в своём EDT-scope, `launch {}`-прыжок исчез. Мутабельные поля view-state (`loadedUrl`, `pageLoads`,
  `isPageLoading`, `serverStatusText`) оставлены: свести их в `combine` + reducer это отдельная переделка UI, не
  владение ресурсом. `PageLoadListener` удалён.
- 2026-09-06: smoke в sandbox (Robot, копия EmpireSmp): открытие файла → скан → host → `kobwebStart` → `Running`
  без клика, переключение файла пересчитало target, SEVERE от плагина 0. Но после закрытия проекта сервер на 8086
  остался: добавлен пункт 6.
- 2026-09-06: пункт 6 сделан и подтверждён вторым и третьим smoke. Все пункты плана закрыты. Полный прогон:
  `./gradlew detekt test :plugin:buildPlugin` зелёный.
