package com.makeevrserg.compose.html.preview.feature.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.core.lifecycle.CoroutineLifecycle
import com.makeevrserg.compose.html.preview.core.lifecycle.Lifecycle
import com.makeevrserg.compose.html.preview.harness.di.HarnessModule
import com.makeevrserg.compose.html.preview.host.di.HostModule
import com.makeevrserg.compose.html.preview.notification.IntellijPreviewNotifier
import com.makeevrserg.compose.html.preview.psi.di.PsiModule
import com.makeevrserg.compose.html.preview.server.di.ServerModule
import com.makeevrserg.compose.html.preview.source.EditorTracker
import com.makeevrserg.compose.html.preview.source.ScanScheduler
import com.makeevrserg.compose.html.preview.source.SourceChangeTracker
import com.makeevrserg.compose.html.preview.source.ToolWindowVisibilityTracker
import com.makeevrserg.compose.html.preview.toolwindow.IntellijPreviewToolWindowPresenter
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.milliseconds

/**
 * The preview store with its IDE ports and the trackers that feed it editor and tool window events.
 * Every tracker is a coroutine that owns its IDE listeners, so enabling the module starts them and
 * disabling it cancels them; no disposable is involved. A tracker that fails is reported by the
 * platform and does not take the others down.
 */
class IntellijPreviewModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule,
    psiModule: PsiModule,
    serverModule: ServerModule,
    hostModule: HostModule,
    harnessModule: HarnessModule
) {
    val previewModule = PreviewModule(
        coreModule = coreModule,
        serverModule = serverModule,
        previewHostLocator = hostModule.previewHostLocator,
        previewHarness = harnessModule.previewHarness,
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
        contract = previewModule.previewStore,
        mainContext = coreModule.dispatchers.main
    )

    val lifecycle: Lifecycle = CoroutineLifecycle(coreModule.backgroundCoroutineFeature) {
        supervisorScope {
            launch { editorTracker.track() }
            launch { sourceChangeTracker.track() }
            launch { toolWindowVisibilityTracker.track() }
        }
    }

    private companion object {
        val RESCAN_DEBOUNCE = 300.milliseconds
    }
}
