package com.makeevrserg.compose.html.preview.feature.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.feature.PreviewFeature
import com.makeevrserg.compose.html.preview.feature.PreviewStateReducer
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.PreviewToolWindowPresenter
import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.notification.PreviewNotifier
import com.makeevrserg.compose.html.preview.server.di.ServerModule
import com.makeevrserg.compose.html.preview.url.PreviewUrlFactory

class PreviewModule(
    coreModule: CoreModule,
    serverModule: ServerModule,
    previewHostLocator: PreviewHostLocator,
    toolWindowPresenter: PreviewToolWindowPresenter,
    previewNotifier: PreviewNotifier
) {
    val previewStore: PreviewStore = PreviewFeature(
        devServerController = serverModule.devServerController,
        hostLocator = previewHostLocator,
        toolWindowPresenter = toolWindowPresenter,
        previewNotifier = previewNotifier,
        reducer = PreviewStateReducer(
            clock = coreModule.clock,
            previewUrlFactory = PreviewUrlFactory()
        ),
        coroutineFeature = coreModule.mainCoroutineFeature
    )
}
