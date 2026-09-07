package com.makeevrserg.compose.html.preview.ui

import com.makeevrserg.compose.html.preview.feature.PageState
import com.makeevrserg.compose.html.preview.feature.PreviewScan
import com.makeevrserg.compose.html.preview.feature.PreviewSourceSetCatalog
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState
import com.makeevrserg.compose.html.preview.ui.state.PreviewMessage

/**
 * Wording of the card that takes the place of the page, for every state that has no page to show.
 * Kept apart from the composables that render it and from the factory that picks it.
 */
class PreviewMessageTexts {

    val noSelectedFile: PreviewMessage = PreviewMessage(
        title = "No file selected",
        description = "Open a Kotlin file with @Preview functions to see them rendered here."
    )

    val lookingForHost: PreviewMessage = PreviewMessage(
        title = "Looking for a module",
        description = "Finding the module of this project whose dev server can render these previews…"
    )

    /** Takes the place of the page on IDE runtimes without the embedded browser. */
    val unsupportedBrowser: PreviewMessage = PreviewMessage(
        title = "No embedded browser",
        description = "JCEF is not available in this IDE runtime. Use Open in Browser to view the preview."
    )

    fun scanning(fileName: String): PreviewMessage {
        return PreviewMessage(
            title = "Looking for previews",
            description = "Reading $fileName…"
        )
    }

    fun startingServer(host: PreviewHost): PreviewMessage {
        return PreviewMessage(
            title = "Starting the dev server",
            description = "Running ${host.kind.startTask} in ${host.displayName}. " +
                "Its output is in the Run tool window."
        )
    }

    fun connecting(host: PreviewHost): PreviewMessage {
        return PreviewMessage(
            title = "Waiting for the dev server",
            description = "Connecting to the dev server of ${host.displayName}…"
        )
    }

    fun pageLoading(host: PreviewHost): PreviewMessage {
        return PreviewMessage(
            title = "Loading the preview",
            description = "Waiting for the page of ${host.displayName}. A dev server that is rebuilding " +
                "the bundle answers only once it is done."
        )
    }

    fun noPreviews(fileName: String): PreviewMessage {
        return PreviewMessage(
            title = "No previews in $fileName",
            description = "A preview is a top-level @Composable function without parameters, " +
                "annotated with @Preview."
        )
    }

    fun unsupportedSourceSet(fileName: String, scan: PreviewScan.UnsupportedSourceSet): PreviewMessage {
        val supported = PreviewSourceSetCatalog.previewSourceSets.joinToString(SOURCE_SET_SEPARATOR)
        return PreviewMessage(
            title = "Previews of $fileName are not compiled to Kotlin/JS",
            description = "They are in ${scan.sourceSetName}, and only previews from $supported end up " +
                "in the page this plugin renders. Move them to a source set of the Kotlin/JS compilation."
        )
    }

    fun hostNotFound(host: PreviewHostResolution.NotFound): PreviewMessage {
        return PreviewMessage(
            title = "Nothing can render this preview",
            description = host.reason
        )
    }

    fun serverFailed(serverState: DevServerState.Failed): PreviewMessage {
        return PreviewMessage(
            title = "The dev server failed",
            description = "${serverState.reason}\n\nPress Refresh Preview to try again."
        )
    }

    fun pageFailed(pageState: PageState.Failed): PreviewMessage {
        return PreviewMessage(
            title = "The preview page could not be loaded",
            description = "${pageState.reason}\n\nPress Refresh Preview to try again."
        )
    }

    fun foreignPage(serverState: DevServerState): PreviewMessage {
        val server = (serverState as? DevServerState.Running)?.host?.displayName
        val where = if (server == null) "The dev server" else "The dev server of $server"
        return PreviewMessage(
            title = "The dev server does not serve the preview page",
            description = "$where answered with something else, which is what a server that was already " +
                "running before the plugin attached to it does: it was built without the page.\n\n" +
                "Press Restart Dev Server to build and start it again."
        )
    }

    private companion object {
        const val SOURCE_SET_SEPARATOR = ", "
    }
}
