package com.makeevrserg.kaleidos.ui.state.model

import com.makeevrserg.kaleidos.feature.model.PageState
import com.makeevrserg.kaleidos.feature.model.PreviewFunction
import com.makeevrserg.kaleidos.feature.model.PreviewScan
import com.makeevrserg.kaleidos.feature.model.PreviewState
import com.makeevrserg.kaleidos.feature.model.PreviewTarget
import com.makeevrserg.kaleidos.feature.model.SourceState
import com.makeevrserg.kaleidos.host.model.DevServerKind
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.host.model.PreviewHostResolution
import com.makeevrserg.kaleidos.server.model.DevServerState

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
                fileName = FILE_NAME,
                isPrivate = false
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

    /** A running server that serves the page, with the browser in [pageState]. */
    fun pageState(pageState: PageState, sourceState: SourceState = SourceState.UpToDate): PreviewState {
        return state(
            previewUrl = PREVIEW_URL,
            sourceState = sourceState,
            serverState = running,
            pageState = pageState
        )
    }

    /** The state in which the browser shows the page: server running, page loaded. */
    fun shownPageState(sourceState: SourceState = SourceState.UpToDate): PreviewState {
        return pageState(PageState.Shown, sourceState)
    }
}
