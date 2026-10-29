package com.makeevrserg.kaleidos.feature.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.feature.api.PreviewStore
import com.makeevrserg.kaleidos.feature.api.PreviewToolWindowPresenter
import com.makeevrserg.kaleidos.feature.component.PreviewFeature
import com.makeevrserg.kaleidos.feature.component.PreviewStateReducer
import com.makeevrserg.kaleidos.feature.service.PreviewServerLauncher
import com.makeevrserg.kaleidos.harness.api.PreviewHarness
import com.makeevrserg.kaleidos.host.api.PreviewHostLocator
import com.makeevrserg.kaleidos.notification.api.PreviewNotifier
import com.makeevrserg.kaleidos.server.di.ServerModule
import com.makeevrserg.kaleidos.source.api.ScanScheduler
import com.makeevrserg.kaleidos.source.service.DefaultScanScheduler
import kotlin.time.Duration.Companion.milliseconds

class PreviewModule(
    coreModule: CoreModule,
    serverModule: ServerModule,
    previewHostLocator: PreviewHostLocator,
    previewHarness: PreviewHarness,
    toolWindowPresenter: PreviewToolWindowPresenter,
    previewNotifier: PreviewNotifier
) {
    val scanScheduler: ScanScheduler = DefaultScanScheduler(rescanDebounce = RESCAN_DEBOUNCE)

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
        reducer = PreviewStateReducer(clock = coreModule.clock),
        coroutineFeature = coreModule.mainCoroutineFeature
    )

    private companion object {
        val RESCAN_DEBOUNCE = 300.milliseconds
    }
}
