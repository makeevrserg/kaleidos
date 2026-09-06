package com.makeevrserg.compose.html.preview.host

/**
 * How a module serves the generated preview page, and what has to happen to stop it again.
 *
 * @param arguments `--continuous` and `-t` keep Gradle recompiling on changes so the page live-reloads
 * @param isServerDetached true when the server is a separate process that outlives the Gradle run and
 * has to be stopped on its own; false when killing the run is enough
 */
enum class DevServerKind(
    val startTask: String,
    val arguments: String,
    val isServerDetached: Boolean
) {
    KOBWEB(startTask = "kobwebStart", arguments = "-t", isServerDetached = true),
    WEBPACK(startTask = "jsBrowserDevelopmentRun", arguments = "--continuous", isServerDetached = false)
}
