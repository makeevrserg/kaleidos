package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import com.makeevrserg.compose.html.preview.server.DevServerState

object PreviewFixtures {
    const val CARD_FILE = "/project/ui/src/jsMain/kotlin/app/CardPreview.kt"
    const val BUTTON_FILE = "/project/ui/src/jsMain/kotlin/app/ButtonPreview.kt"

    val previewHost = PreviewHost(
        gradlePath = ":instances:web-preview",
        directory = "/project/instances/web-preview",
        rootProjectPath = "/project",
        kind = DevServerKind.WEBPACK
    )

    val otherHost = previewHost.copy(gradlePath = ":instances:web-app", directory = "/project/instances/web-app")

    val hostFound: PreviewHostResolution = PreviewHostResolution.Found(previewHost)

    val running: DevServerState = DevServerState.Running(previewHost, "http://localhost:8085")

    fun previewFunction(name: String, filePath: String = CARD_FILE): PreviewFunction {
        return PreviewFunction(
            fqn = "app.$name",
            name = name,
            filePath = filePath,
            fileName = filePath.substringAfterLast('/')
        )
    }

    fun target(
        filePath: String = CARD_FILE,
        scan: PreviewScan = PreviewScan.Renderable(listOf(previewFunction("CardPreview", filePath))),
        focusedFqn: String? = null,
        host: PreviewHostResolution? = null
    ): PreviewTarget {
        return PreviewTarget(
            filePath = filePath,
            fileName = filePath.substringAfterLast('/'),
            scan = scan,
            focusedFqn = focusedFqn,
            host = host
        )
    }

    fun renderable(vararg names: String, filePath: String = CARD_FILE): PreviewScan.Renderable {
        return PreviewScan.Renderable(names.map { name -> previewFunction(name, filePath) })
    }

    fun initialState(): PreviewState {
        return PreviewState(
            target = null,
            previewUrl = null,
            isToolWindowVisible = false,
            sourceState = SourceState.UpToDate,
            serverState = DevServerState.Stopped,
            pageState = PageState.Loading
        )
    }
}
