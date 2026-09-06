package com.makeevrserg.compose.html.preview.host

/**
 * How a preview module serves its page, detected from the Gradle tasks of the module. Kobweb comes
 * first: a Kobweb application also exposes the plain Kotlin/JS tasks, but only its own server
 * initialises Silk.
 *
 * @param arguments `--continuous` and `-t` keep Gradle recompiling on changes so the page live-reloads
 * @param stopTask task that stops a server outliving the Gradle run; null when killing the run is enough
 */
enum class DevServerKind(
    val startTask: String,
    val arguments: String,
    val stopTask: String?
) {
    KOBWEB(startTask = "kobwebStart", arguments = "-t", stopTask = "kobwebStop"),
    WEBPACK(startTask = "jsBrowserDevelopmentRun", arguments = "--continuous", stopTask = null);

    companion object {
        fun detect(taskNames: Set<String>): DevServerKind? {
            return entries.firstOrNull { kind -> kind.startTask in taskNames }
        }
    }
}
