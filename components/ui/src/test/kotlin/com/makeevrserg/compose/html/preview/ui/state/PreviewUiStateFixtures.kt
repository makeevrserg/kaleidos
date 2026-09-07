package com.makeevrserg.compose.html.preview.ui.state

import com.makeevrserg.compose.html.preview.feature.PreviewFunction
import com.makeevrserg.compose.html.preview.feature.PreviewState
import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.feature.SourceState
import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState

object PreviewUiStateFixtures {
    const val FILE_NAME = "CardPreview.kt"
    const val PREVIEW_URL = "http://localhost:8085/?preview=app.CardPreview"

    val host = PreviewHost(
        gradlePath = ":instances:web-preview",
        directory = "/project/instances/web-preview",
        rootProjectPath = "/project",
        kind = DevServerKind.WEBPACK
    )

    fun target(
        previews: List<PreviewFunction> = listOf(
            PreviewFunction(
                fqn = "app.CardPreview",
                name = "CardPreview",
                filePath = "/project/ui/src/jsMain/kotlin/app/$FILE_NAME",
                fileName = FILE_NAME
            )
        ),
        host: PreviewHostResolution? = PreviewHostResolution.Found(PreviewUiStateFixtures.host)
    ): PreviewTarget {
        return PreviewTarget(
            filePath = "/project/ui/src/jsMain/kotlin/app/$FILE_NAME",
            fileName = FILE_NAME,
            previews = previews,
            focusedFqn = null,
            host = host
        )
    }

    fun state(
        target: PreviewTarget? = target(),
        previewUrl: String? = null,
        sourceState: SourceState = SourceState.UpToDate,
        serverState: DevServerState = DevServerState.Stopped
    ): PreviewState {
        return PreviewState(
            target = target,
            previewUrl = previewUrl,
            isToolWindowVisible = true,
            sourceState = sourceState,
            serverState = serverState
        )
    }
}
