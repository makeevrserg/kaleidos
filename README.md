## Compose HTML Preview

IntelliJ plugin that previews [Compose HTML](https://github.com/JetBrains/compose-multiplatform#compose-html) (DOM)
composables inside the IDE. The official Compose preview renders through the Android backend and cannot draw DOM,
so this plugin renders the composable in the embedded browser instead.

Found bug or need a new feature? Please submit a

- [💀 Bug report](https://github.com/makeevrserg/MVIKotlin-Decompose-Plugin/issues/new?assignees=makeevrserg&labels=bug&projects=&template=bug.md&title=)
- [👾 Feature request](https://github.com/makeevrserg/MVIKotlin-Decompose-Plugin/issues/new?assignees=makeevrserg&labels=enhancement&projects=&template=feature.md&title=)

### How it works

1. The **Compose HTML Preview** tool window follows the editor: whenever a Kotlin file is selected, its top-level
   `@Preview @Composable` functions without parameters are collected and shown together, in source order, like
   in the official Compose preview. Editing the file rescans it, so new previews appear without any click.
2. Nothing is rendered and no dev server is started while the tool window is hidden. Opening it renders the
   current file right away.
3. The plugin picks the module that renders the previews from the Gradle structure of the project (see below),
   the way the official Compose preview finds the Gradle module of a file. Nothing is configured.
4. The plugin then **generates the preview application itself** into the `build` directories of the modules
   involved and passes those generated sources to the run through a Gradle init script. No file of your project
   is touched, and no other build of the project sees them (see below).
5. If the dev server of the picked module is not running, the plugin starts it in the Run tool window:
   `kobwebStart -t` for a Kobweb application, `jsBrowserDevelopmentRun --continuous` for a plain Kotlin/JS
   module. The origin is read from the output of the server (`Loopback: http://localhost:8305/`,
   `A Kobweb server is now running at http://localhost:8086`).
6. The generated page is loaded in JCEF with the previews of the file in the query:
   `http://localhost:{port}/compose-html-preview.html?preview={fqn,fqn}` for a plain Kotlin/JS module,
   `http://localhost:{port}/compose-html-preview?preview={fqn,fqn}` for Kobweb. Without the parameter the same
   page shows every preview it knows, so it doubles as a gallery.
7. Every preview function also gets a gutter icon. Clicking it opens the tool window and appends `#<fqn>` to the
   URL so the page scrolls to that preview.
8. Because the dev server runs in continuous mode, saving a file recompiles the bundle and the page live-reloads.

### Which module renders the previews

For the selected file the plugin takes the Gradle module the file belongs to and decides from the tasks the last
Gradle sync recorded:

- **A Kotlin/JS module with a browser target** (`js { browser() }`, `jsBrowserTest` in the sync) renders its own
  files, library or application. For the preview run the module becomes an application: the plugin adds
  `binaries.executable()`, leaves the `main` of the module out of that compilation, because the generated page
  brings its own, and picks a free port between 8300 and 8399 so the preview never collides with the real
  application of the project.
- **A Kobweb library** cannot render its own components: the code that registers the styles of Silk components is
  generated for Kobweb applications only. Any Kobweb application of the build that depends on the module can
  serve it; a module whose Gradle path contains `preview` wins, otherwise the shortest path, so the same one is
  picked every time. The generated page becomes a route of that site, which is why previews get the `@App` root
  of the real site: theme and Silk exactly as in production. Its port comes from `.kobweb/conf.yaml`.
- **A module without a Kotlin/JS target**, for example previews in a module shared with other platforms, is
  rendered by a module that depends on it, by the same rules.
- When nothing qualifies, the tool window says what was found and why it cannot render the file. A Gradle sync
  that has not finished yet gives its own message; press **Refresh Preview** once it is done.

A dev server that already answers is reused instead of started again. The server the plugin started for the previous
module is stopped, so the plugin owns at most one dev server per project. Closing the project stops that server as
well: a run started by the plugin never outlives it.

### What the tool window shows

Switching editor tabs drops the page immediately: the preview of the file you left is gone before the new file has
even been read, and a page is put on screen only once the browser reports that it loaded and the page itself
answered that it is the generated one, never while it is still loading. Anything that is not a page is a card in
the shape the platform uses for empty and failed states: an icon, one line saying what is going on and the detail
under it.

| Icon | Title | When |
|---|---|---|
| Spinner | Looking for previews | the selected file is being scanned |
| Spinner | Looking for a module | the Gradle structure is being read for a module that can render the file |
| Spinner | Starting the dev server | the Gradle task of that module is running |
| Spinner | Waiting for the dev server | the plugin is polling the port of a server that has not answered yet |
| Spinner | Loading the preview | the page is being loaded into the embedded browser |
| — | *the page* | the browser loaded the page and the page identified itself |
| Information | No file selected | no editor file is selected |
| Information | No previews in … | the file declares no `@Preview` function |
| Information | Previews of … are not compiled to Kotlin/JS | they are in `jvmMain` or another source set that never reaches the page |
| Error | Nothing can render this preview | no module of the build can serve the file; the detail says why |
| Error | The dev server failed | the Gradle run failed; **Refresh Preview** tries again |
| Error | The preview page could not be loaded | the browser could not load the address: connection refused, an HTTP error, and so on |
| Error | The dev server does not serve the preview page | the address answered with something that is not the generated page |

The last one is what a dev server that was already running before the plugin attached to it does: it was built
without the preview page, and a single page application answers *every* path with the shell of the real site, so
the address returns HTTP 200 and renders nothing. The generated page therefore names itself in the document title,
and a load that never shows that title is reported instead of an empty panel. **Restart Dev Server** builds and
starts the server again with the page in it.

A page the browser could not load stays loaded behind the message, so a rebuild that fixes the problem brings it
back on its own live reload; **Refresh Preview** retries at once.

### What the plugin generates

Everything generated lives under `build/compose-html-preview` of the module it belongs to, so it is ignored by
version control, removed by `clean` and never mixed into your sources:

- `kotlin/composehtmlpreview/generated/<module>/PreviewRegistry.kt` in **every module that owns previews** the
  picked module can see. The registry is compiled into that module, so `internal` previews work: only the object
  that dispatches them is public.
- `kotlin/composehtmlpreview/generated/PreviewPage.kt` in the picked module, plus `PreviewMain.kt` and
  `compose-html-preview.html` for a plain Kotlin/JS module or `ComposeHtmlPreviewRoute.kt` with
  `@Page("/compose-html-preview")` for a Kobweb application.
- `compose-html-preview.init.gradle`, the init script that adds those source directories to the build. It is
  passed to the preview run as `--init-script`, so it affects nothing else: not your build files, not the Gradle
  sync of the IDE, not a build you start yourself.

Previews are collected from source sets that end up in the Kotlin/JS compilation — `commonMain`, `jsMain` and the
`main` of the `kotlin("js")` plugin — and never from tests. The generated sources are rewritten before every
launch and only when their content actually changed, so a preview you add is on the page after the next
recompilation, while switching between two files recompiles nothing.

### Stale previews

Any edit of a Kotlin source in the project marks the page as stale: a yellow banner says which file changed and when.
The plugin does not compute the dependency graph, so a preview is considered affected by any Kotlin change. When the
dev server finishes rebuilding and the page live-reloads, the banner turns green with the reload time. If the banner
stays yellow, check the Run tool window: the rebuild has probably failed with a compilation error.

### What your project has to provide

Nothing beyond what a Compose HTML project has anyway: a module with a Kotlin/JS browser target that depends on
Compose HTML (`org.jetbrains.compose.html:html-core`) and the Compose compiler plugin, which is what makes the
`@Preview @Composable` functions compile in the first place. There is no preview module to write, no registry to
maintain and no page to serve: the plugin generates all of it.

Two things are worth knowing:

- The `main` of the module that renders the previews is left out of the compilation of the preview run. If that
  file also declares something else the module needs, the run fails with a compilation error in the Run tool
  window; move the `main` into a file of its own.
- A dev server that was started outside the IDE, from a terminal for example, is adopted as it is. It was built
  without the generated page, so the tool window says that the server does not serve it; press **Restart Dev
  Server** to have the plugin run it.

### Kobweb projects

Kobweb components depend on Silk, whose styles are registered by the entry point the Kobweb Gradle plugin generates
for an application, so previews of a Kobweb library are served by a Kobweb application of the build. The plugin adds
its page to that application as a `@Page("/compose-html-preview")` route, which the Kobweb code generation picks up
from the generated source directory like any page of the site, and runs `kobwebStart -t`; the port comes from
`server.port` in `.kobweb/conf.yaml` of the module.

The Kobweb server is a separate process that outlives the Gradle run, so stopping the run alone would leave the
server on the port. The plugin does what `kobwebStop` does, without Gradle: it reads the process id from
`.kobweb/server/state.yaml` of the module, asks the process to terminate and kills it after 10 seconds if it has
not. This also works while the project is closing, when no Gradle task can run any more. The plugin also re-checks
the port when the Gradle task finishes successfully, so a server that keeps running is shown as running.

### Tool window actions

- **Refresh Preview** reloads the page and re-checks the dev server.
- **Open in Browser** opens the current page URL in the system browser. This is the fallback when JCEF is not
  available, for example under Remote Development.
- Gear menu: **Restart Dev Server**, **Stop Dev Server**, **Open DevTools**.

Only runs started by the plugin are stopped, whether by **Stop Dev Server**, by a switch to another module or by
closing the project; a dev server started from a terminal is left untouched.

### Development

- `./gradlew :plugin:runIde` starts a sandbox IDE with the plugin.
- `./gradlew :plugin:buildPlugin` builds the distribution into `plugin/build/distributions`.
- `./gradlew :plugin:verifyPlugin` runs the IntelliJ Plugin Verifier.
- `./gradlew detekt` runs static analysis.
- `./gradlew test` runs the unit tests of the plain Kotlin components.

Requirements: IntelliJ IDEA 2026.2 or newer, JDK 25 to build.

#### Modules

`:plugin` holds `plugin.xml`, the resources and the IntelliJ entry points: the line marker contributor, the gutter
action, the tool window factory, the startup activity and the project service. Everything else lives under
`:components`. A component with an `api` module is plain Kotlin: it compiles against the stdlib and coroutines
bundled with the platform, knows nothing about IntelliJ and is tested with `kotlin.test`. Its `intellij` module
holds the adapters that implement the ports of the `api` module with platform APIs.

| Component | `api` | `intellij` |
|---|---|---|
| `core` | coroutine features, `Lifecycle`, `PreviewDispatchers` | `ProjectDependencies`, EDT dispatchers |
| `host` | host models, `PreviewHostSelector`, `PreviewProjectStructureReader` and `PreviewHostLocator` ports | IDE module model readers |
| `server` | `DevServerLauncher` (one run as a cold flow), `DevServerController`, output parsing, `GradleTaskRunner` port | `ExternalSystemUtil` runner |
| `harness` | generators of the registry, the page and the init script, the plan behind them, `ProjectPreviewSource` port | |
| `preview` | `PreviewStore` contract, state, reducer, `PreviewFeature`, notifier and tool window ports | editor and tool window trackers, port implementations |
| `psi` | | `@Preview` detection on Kotlin PSI, project-wide preview scan |
| `ui` | | tool window panel, JCEF browser, actions |

Every component is merged into the plugin JAR through `pluginComposedModule` in `plugin/build.gradle.kts`.

#### Dependency injection

Dependencies are wired by hand through constructors; there is no DI framework and no service locator inside the
components. Each component has a `di/…Module` class that builds its object graph from the modules it depends on and
exposes the services other components need. `RootModule` in `:plugin` creates the modules in dependency order and
aggregates their `Lifecycle`s. The project-level `PreviewProjectService` owns the `RootModule`; platform entry points,
which IntelliJ creates without constructors, reach the graph through that service only.

### Gratitude

- [Compose Multiplatform IDE plugin](https://github.com/JetBrains/compose-multiplatform/tree/v1.7.3/idea-plugin) for the gutter and Gradle orchestration approach
- [kotlin-js-preview-idea-plugin](https://github.com/sanyavertolet/kotlin-js-preview-idea-plugin) for proving Kotlin/JS previews in JCEF work
- [jetbrains](https://jetbrains.com) for IntelliJ
