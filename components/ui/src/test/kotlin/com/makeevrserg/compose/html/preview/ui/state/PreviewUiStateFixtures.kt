package com.makeevrserg.compose.html.preview.ui.state

import com.makeevrserg.compose.html.preview.feature.PageState
import com.makeevrserg.compose.html.preview.feature.PreviewFunction
import com.makeevrserg.compose.html.preview.feature.PreviewScan
import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState

object PreviewUiStateFixtures {
    const val FILE_NAME = "CardPreview.kt"
    const val FILE_PATH = "/project/ui/src/jsMain/kotlin/app/$FILE_NAME"
    const val PREVIEW_URL = "http://localhost:8085/?preview=app.CardPreview"

    val host = PreviewHost(
        gradlePath = ":instances:web-preview",
        directory = "/project/instances/web-preview",
        rootProjectPath = "/project",
        kind = DevServerKind.WEBPACK
    )

    val renderable = PreviewScan.Renderable(
        listOf(
            PreviewFunction(
                fqn = "app.CardPreview",
                name = "CardPreview",
                filePath = FILE_PATH,
                fileName = FILE_NAME
            )
        )
    )

    val running: DevServerState = DevServerState.Running(host, "http://localhost:8085")

    fun target(
        scan: PreviewScan = renderable,
        host: PreviewHostResolution? = PreviewHostResolution.Found(PreviewUiStateFixtures.host)
    ): PreviewTarget {
        return PreviewTarget(
            filePath = FILE_PATH,
            fileName = FILE_NAME,
            scan = scan,
            focusedFqn = null,
            host = host
        )
    }

    fun state(
        target: PreviewTarget? = target(),
        previewUrl: String? = null,
        sourceState: SourceState = SourceState.UpToDate,
        serverState: DevServerState = DevServerState.Stopped,
        pageState: PageState = PageState.Loading
    ): PreviewState {
        return PreviewState(
            target = target,
            previewUrl = previewUrl,
            isToolWindowVisible = true,
            sourceState = sourceState,
            serverState = serverState,
            pageState = pageState
        )
    }

    /** The state in which the browser shows the page: server running, page loaded. */
    fun shownPageState(sourceState: SourceState = SourceState.UpToDate): PreviewState {
        return state(
            previewUrl = PREVIEW_URL,
            sourceState = sourceState,
            serverState = running,
            pageState = PageState.Shown
        )
    }
}
