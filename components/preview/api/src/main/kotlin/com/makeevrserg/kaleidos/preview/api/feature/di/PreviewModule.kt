package com.makeevrserg.kaleidos.preview.api.feature.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.harness.api.api.PreviewHarness
import com.makeevrserg.kaleidos.host.api.api.PreviewHostLocator
import com.makeevrserg.kaleidos.preview.api.feature.api.PreviewStore
import com.makeevrserg.kaleidos.preview.api.feature.api.PreviewToolWindowPresenter
import com.makeevrserg.kaleidos.preview.api.feature.component.PreviewFeature
import com.makeevrserg.kaleidos.preview.api.feature.component.PreviewStateReducer
import com.makeevrserg.kaleidos.preview.api.feature.usecase.LaunchPreviewServerUseCase
import com.makeevrserg.kaleidos.preview.api.notification.api.PreviewNotifier
import com.makeevrserg.kaleidos.preview.api.source.api.ScanScheduler
import com.makeevrserg.kaleidos.preview.api.source.service.DefaultScanScheduler
import com.makeevrserg.kaleidos.server.api.di.ServerModule
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
        serverLauncher = LaunchPreviewServerUseCase(
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
