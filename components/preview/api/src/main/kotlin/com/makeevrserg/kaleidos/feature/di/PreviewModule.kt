package com.makeevrserg.kaleidos.feature.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.feature.PreviewFeature
import com.makeevrserg.kaleidos.feature.PreviewServerLauncher
import com.makeevrserg.kaleidos.feature.PreviewStateReducer
import com.makeevrserg.kaleidos.feature.PreviewStore
import com.makeevrserg.kaleidos.feature.PreviewToolWindowPresenter
import com.makeevrserg.kaleidos.harness.PreviewHarness
import com.makeevrserg.kaleidos.host.PreviewHostLocator
import com.makeevrserg.kaleidos.notification.PreviewNotifier
import com.makeevrserg.kaleidos.server.di.ServerModule
import com.makeevrserg.kaleidos.url.PreviewUrlFactory

class PreviewModule(
    coreModule: CoreModule,
    serverModule: ServerModule,
    previewHostLocator: PreviewHostLocator,
    previewHarness: PreviewHarness,
    toolWindowPresenter: PreviewToolWindowPresenter,
    previewNotifier: PreviewNotifier
) {
    val previewStore: PreviewStore = PreviewFeature(
        devServerController = serverModule.devServerController,
        serverLauncher = PreviewServerLauncher(
            previewHarness = previewHarness,
            devServerController = serverModule.devServerController,
            previewNotifier = previewNotifier
        ),
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
