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
3. The plugin finds the module that serves the previews of the file from the Gradle structure of the project
   (see below), the way the official Compose preview finds the Gradle module of a file. Nothing is configured.
4. If the dev server of that module is not running, the plugin starts it in the Run tool window:
   `kobwebStart -t` for a Kobweb application, `jsBrowserDevelopmentRun --continuous` for a plain Kotlin/JS module.
   The port is taken from the output of the server (`Loopback: http://localhost:8085/`,
   `A Kobweb server is now running at http://localhost:8086`), so it can be whatever the module configures.
5. The page `http://localhost:{port}/?preview={fqn}` is loaded in JCEF, where `{fqn}` is the comma-separated list
   of fully qualified names of the previews in the file.
6. Every preview function also gets a gutter icon. Clicking it opens the tool window and appends `#<fqn>` to the
   URL so the page scrolls to that preview.
7. Because the dev server runs in continuous mode, saving a file recompiles the bundle and the page live-reloads.

### How the preview module is found

For the selected file the plugin takes the Gradle module the file belongs to and looks at every module of the
build that has a dev server task: `kobwebStart` (Kobweb application) or `jsBrowserDevelopmentRun` (Kotlin/JS
browser module). A module qualifies when it is the file's own module or depends on it, directly or transitively,
according to the module dependencies imported by the last Gradle sync.

- One qualifying module: it serves the preview.
- Several, for example the real sites of the project plus the preview application: the module whose Gradle path
  contains `preview` wins, and among those the file's own module.
- Still several, or none: the tool window explains what was found and what to change. A Gradle sync that has not
  finished yet gives the same message; press **Refresh Preview** once it is done.

A Kobweb server that is already running is reused: its port is read from `.kobweb/conf.yaml` of the module, so a
server left from a terminal or an earlier IDE session is adopted instead of started again. Switching to a file of
another module clears the page at once and shows a spinner until that module's server answers; the server the plugin
started for the previous module is stopped, so the plugin owns at most one dev server per project. Closing the
project stops that server as well: a run started by the plugin never outlives it.

### Stale previews

Any edit of a Kotlin source in the project marks the page as stale: a yellow banner says which file changed and when.
The plugin does not compute the dependency graph, so a preview is considered affected by any Kotlin change. When the
dev server finishes rebuilding and the page live-reloads, the banner turns green with the reload time. If the banner
stays yellow, check the Run tool window: the rebuild has probably failed with a compilation error.

### What your project has to provide

The plugin does not compile anything itself. Your project needs a Kotlin/JS browser module that:

- depends on the modules that contain the `@Preview` functions;
- has `preview` in its Gradle path when other applications of the build depend on the same modules,
  for example `:instances:web-preview`;
- serves a page which reads the `preview` query parameter, a comma-separated list of fully qualified names, and
  renders every matching composable through `renderComposable`, one section per preview with `id` equal to the
  FQN so the URL fragment can scroll to it.

Kotlin/JS has no annotation reflection, so the mapping from a function name to a composable has to be generated.
A KSP processor over `@Preview` functions or a hand-written registry both work. A minimal registry looks like this:

```kotlin
data class PreviewEntry(val fqn: String, val content: @Composable () -> Unit)

val previews = listOf(
    PreviewEntry("com.example.ui.CardPreview") { CardPreview() },
)

fun main() {
    val requested = URLSearchParams(window.location.search).get("preview").orEmpty().split(",")
    renderComposable(rootElementId = "root") {
        requested.forEach { fqn ->
            Section(attrs = { id(fqn) }) {
                previews.firstOrNull { entry -> entry.fqn == fqn }?.content?.invoke()
                    ?: Text("Preview $fqn is not registered")
            }
        }
    }
}
```

The webpack dev server may use any port; the plugin reads it from the output of `jsBrowserDevelopmentRun`. Pick a
fixed one only if the default `8080` collides with another server of the project:

```kotlin
kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                devServer = devServer?.copy(port = 8085)
            }
        }
    }
}
```

### Kobweb projects

Kobweb components depend on Silk, whose initialization is generated by the Kobweb Gradle plugin, so the preview page
has to be a small Kobweb application: the same `@App` root as your site (theme, Silk) and one `@Page("/")` that
reads the `preview` parameter. The Kobweb dev server then plays the role of the webpack dev server, including live
reload. The plugin recognises such a module by its `kobwebStart` task and runs `kobwebStart -t`; the port comes
from `server.port` in `.kobweb/conf.yaml` of the module. Give the module a distinct port so it does not collide
with the real site.

The Kobweb server is a separate process that outlives the Gradle run, so **Stop Dev Server** runs `kobwebStop`
for it: without that, stopping the run in the IDE would leave the server on the port. The plugin also re-checks
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

Requirements: IntelliJ IDEA 2025.1 or newer, JDK 21 to build.

#### Modules

`:plugin` holds `plugin.xml`, the resources and the IntelliJ entry points: the line marker contributor, the gutter
action, the tool window factory, the startup activity and the project service. Everything else lives under
`:components`. A component with an `api` module is plain Kotlin: it compiles against the stdlib and coroutines
bundled with the platform, knows nothing about IntelliJ and is tested with `kotlin.test`. Its `intellij` module
holds the adapters that implement the ports of the `api` module with platform APIs.

| Component | `api` | `intellij` |
|---|---|---|
| `core` | coroutine features, `Lifecycle`, `PreviewDispatchers` | `ProjectDependencies`, EDT dispatchers |
| `host` | host models, `PreviewHostSelector`, `PreviewHostLocator` port | IDE module model readers |
| `server` | `DevServerLauncher` (one run as a cold flow), `DevServerController`, output parsing, `GradleTaskRunner` port | `ExternalSystemUtil` runner |
| `preview` | `PreviewStore` contract, state, reducer, `PreviewFeature`, notifier and tool window ports | editor and tool window trackers, port implementations |
| `psi` | | `@Preview` detection on Kotlin PSI |
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
