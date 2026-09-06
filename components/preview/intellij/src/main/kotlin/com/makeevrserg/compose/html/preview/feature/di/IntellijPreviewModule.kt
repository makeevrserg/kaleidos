package com.makeevrserg.compose.html.preview.feature.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.core.lifecycle.CompositeLifecycle
import com.makeevrserg.compose.html.preview.core.lifecycle.CoroutineLifecycle
import com.makeevrserg.compose.html.preview.core.lifecycle.LambdaLifecycle
import com.makeevrserg.compose.html.preview.core.lifecycle.Lifecycle
import com.makeevrserg.compose.html.preview.host.di.HostModule
import com.makeevrserg.compose.html.preview.notification.IntellijPreviewNotifier
import com.makeevrserg.compose.html.preview.psi.di.PsiModule
import com.makeevrserg.compose.html.preview.server.di.ServerModule
import com.makeevrserg.compose.html.preview.source.EditorTracker
import com.makeevrserg.compose.html.preview.source.ScanScheduler
import com.makeevrserg.compose.html.preview.source.SourceChangeTracker
import com.makeevrserg.compose.html.preview.source.ToolWindowVisibilityTracker
import com.makeevrserg.compose.html.preview.toolwindow.IntellijPreviewToolWindowPresenter
import kotlin.time.Duration.Companion.milliseconds

/**
 * The preview store with its IDE ports and the trackers that feed it editor and tool window events.
 */
class IntellijPreviewModule(
    coreModule: CoreModule,
    private val intellijCoreModule: IntellijCoreModule,
    psiModule: PsiModule,
    serverModule: ServerModule,
    hostModule: HostModule
) {
    val previewModule = PreviewModule(
        coreModule = coreModule,
        serverModule = serverModule,
        previewHostLocator = hostModule.previewHostLocator,
        toolWindowPresenter = IntellijPreviewToolWindowPresenter(intellijCoreModule.projectDependencies),
        previewNotifier = IntellijPreviewNotifier(intellijCoreModule.project)
    )

    private val editorTracker = EditorTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        fileScanner = psiModule.previewFileScanner,
        scanScheduler = ScanScheduler(rescanDebounce = RESCAN_DEBOUNCE),
        contract = previewModule.previewStore,
        mainContext = coreModule.dispatchers.main
    )

    private val sourceChangeTracker = SourceChangeTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        contract = previewModule.previewStore
    )

    private val toolWindowVisibilityTracker = ToolWindowVisibilityTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        contract = previewModule.previewStore
    )

    /**
     * The editor tracker releases its listeners itself when its coroutine is cancelled; the other
     * listeners are unregistered by [IntellijCoreModule.listenerDisposable], so nothing to do on disable.
     */
    val lifecycle: Lifecycle = CompositeLifecycle(
        listOf(
            CoroutineLifecycle(coreModule.backgroundCoroutineFeature) { editorTracker.track() },
            LambdaLifecycle(
                onEnable = {
                    sourceChangeTracker.start(intellijCoreModule.listenerDisposable)
                    toolWindowVisibilityTracker.start(intellijCoreModule.listenerDisposable)
                }
            )
        )
    )

    private companion object {
        val RESCAN_DEBOUNCE = 300.milliseconds
    }
}
