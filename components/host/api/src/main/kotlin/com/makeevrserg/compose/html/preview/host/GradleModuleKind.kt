package com.makeevrserg.compose.html.preview.host

/**
 * What a module of the build can do for a preview, detected from the tasks the Gradle sync recorded.
 * Kobweb comes first: a Kobweb module also has the plain Kotlin/JS tasks, but only the Kobweb server
 * runs the generated code that registers the styles of Silk components.
 *
 * @param devServerKind how the module serves a page, null when it cannot serve one itself
 */
enum class GradleModuleKind(val devServerKind: DevServerKind?) {
    KOBWEB_APPLICATION(DevServerKind.KOBWEB),

    /** Kobweb components need the generated entry point of an application to be styled at all. */
    KOBWEB_LIBRARY(null),

    /** Any Kotlin/JS module with a browser target: the plugin turns it into the preview application. */
    JS_BROWSER(DevServerKind.WEBPACK),

    OTHER(null);

    companion object {
        private const val KOBWEB_APPLICATION_TASK = "kobwebStart"
        private const val KOBWEB_LIBRARY_TASK = "kobwebGenerateLibraryMetadata"

        /** Present exactly when the module declares `js { browser() }`, for libraries as well as applications. */
        private const val JS_BROWSER_TASK = "jsBrowserTest"

        fun detect(taskNames: Set<String>): GradleModuleKind = when {
            KOBWEB_APPLICATION_TASK in taskNames -> KOBWEB_APPLICATION
            KOBWEB_LIBRARY_TASK in taskNames -> KOBWEB_LIBRARY
            JS_BROWSER_TASK in taskNames -> JS_BROWSER
            else -> OTHER
        }
    }
}
