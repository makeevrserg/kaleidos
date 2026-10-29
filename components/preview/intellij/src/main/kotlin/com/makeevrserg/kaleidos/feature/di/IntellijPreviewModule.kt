package com.makeevrserg.kaleidos.feature.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.core.lifecycle.api.Lifecycle
import com.makeevrserg.kaleidos.feature.service.CoroutineLifecycle
import com.makeevrserg.kaleidos.harness.api.PreviewHarness
import com.makeevrserg.kaleidos.host.api.PreviewHostLocator
import com.makeevrserg.kaleidos.notification.service.IntellijPreviewNotifier
import com.makeevrserg.kaleidos.server.di.ServerModule
import com.makeevrserg.kaleidos.source.api.SelectedFileScanner
import com.makeevrserg.kaleidos.source.service.EditorTracker
import com.makeevrserg.kaleidos.source.service.SourceChangeTracker
import com.makeevrserg.kaleidos.source.service.SourceSaveTracker
import com.makeevrserg.kaleidos.source.service.ToolWindowVisibilityTracker
import com.makeevrserg.kaleidos.toolwindow.component.IntellijPreviewToolWindowPresenter
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * The preview store with its IDE ports and the trackers that feed it editor and tool window events.
 * Every tracker is a coroutine that owns its IDE listeners, so enabling the module starts them and
 * disabling it cancels them; no disposable is involved. A tracker that fails is reported by the
 * platform and does not take the others down.
 */
class IntellijPreviewModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule,
    selectedFileScanner: SelectedFileScanner,
    serverModule: ServerModule,
    previewHostLocator: PreviewHostLocator,
    previewHarness: PreviewHarness
) {
    val previewModule = PreviewModule(
        coreModule = coreModule,
        serverModule = serverModule,
        previewHostLocator = previewHostLocator,
        previewHarness = previewHarness,
        toolWindowPresenter = IntellijPreviewToolWindowPresenter(intellijCoreModule.projectDependencies),
        previewNotifier = IntellijPreviewNotifier(intellijCoreModule.project)
    )

    private val editorTracker = EditorTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        fileScanner = selectedFileScanner,
        scanScheduler = previewModule.scanScheduler,
        contract = previewModule.previewStore,
        mainContext = coreModule.dispatchers.main
    )

    private val sourceChangeTracker = SourceChangeTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        contract = previewModule.previewStore
    )

    private val sourceSaveTracker = SourceSaveTracker(
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
            launch { sourceSaveTracker.track() }
            launch { toolWindowVisibilityTracker.track() }
        }
    }
}
