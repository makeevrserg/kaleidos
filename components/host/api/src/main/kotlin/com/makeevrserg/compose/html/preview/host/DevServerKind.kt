package com.makeevrserg.compose.html.preview.host

/**
 * How a preview module serves its page, detected from the Gradle tasks of the module. Kobweb comes
 * first: a Kobweb application also exposes the plain Kotlin/JS tasks, but only its own server
 * initialises Silk.
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
    WEBPACK(startTask = "jsBrowserDevelopmentRun", arguments = "--continuous", isServerDetached = false);

    companion object {
        fun detect(taskNames: Set<String>): DevServerKind? {
            return entries.firstOrNull { kind -> kind.startTask in taskNames }
        }
    }
}
