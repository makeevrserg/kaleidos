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

### 2. `EditorTracker` на `callbackFlow` + `mapLatest` — НЕ НАЧАТО

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

### 3. `PreviewFeature.connectIfNeeded` через `mapLatest` — НЕ НАЧАТО

Проблема. Каждое событие редактора делает `launch { resolveHost(); requestRunning() }`; быстрое переключение файлов
даёт параллельные `readAction`, отменить устаревший `locate` нечем. Состояние не портится (редьюсер сверяет
`filePath`), дублируется только работа. Также `launch { toolWindowPresenter.show() }` в `onFocusPreview`.

Решение. `MutableStateFlow<ConnectRequest?>` (target + retryAfterFailure + attempt) и в `init`
`requests.filterNotNull().mapLatest { resolveHost(); requestRunning() }.launchIn(this)`. Тесты `PreviewFeatureTest`
уже покрывают контракт; добавить тест «второй запрос отменяет первый resolve».

### 4. `PreviewPanel`: загрузки страницы как flow — НЕ НАЧАТО (низкий приоритет)

`PreviewBrowser.pageLoads(): Flow<String>` как `callbackFlow` с `awaitClose { removeLoadHandler }` вместо
`addPageLoadListener`; панель делает `combine(contract.state, browser.pageLoads())` с одним `render`, уходит
`launch {}` для прыжка на EDT и часть мутабельных полей. Утечки нет, JCEF умирает вместе с окном; это стиль.

### 5. Мелочи — НЕ НАЧАТО

- `HttpDevServerHealthCheck.isAlive`: `runCatching` ловит `CancellationException`; заменить на `catch (IOException)`.
- `ToolWindowVisibilityTracker`, `SourceChangeTracker`: перевести на `callbackFlow` только ради единообразия
  после пункта 2, чтобы `IntellijPreviewModule.lifecycle` стал одним `Job`.

Не трогать: `GradleModuleCatalog`, `ModuleDependencyGraphReader`, `IntellijPreviewHostLocator`, `PreviewFileScanner`,
`KobwebConfReader`, actions, line marker — stateless запросы, flow им не нужен.

## Проверка каждого шага

```
./gradlew detekt :components:server:api:test :components:preview:api:test :plugin:compileKotlin
```

Полный прогон перед финалом: `./gradlew detekt test :plugin:buildPlugin`.

## Журнал

- 2026-09-06: аудит проведён, план записан. Пункт 1 в работе.
