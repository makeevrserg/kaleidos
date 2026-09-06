package com.makeevrserg.compose.html.preview.feature.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.core.lifecycle.LambdaLifecycle
import com.makeevrserg.compose.html.preview.core.lifecycle.Lifecycle
import com.makeevrserg.compose.html.preview.host.di.HostModule
import com.makeevrserg.compose.html.preview.notification.IntellijPreviewNotifier
import com.makeevrserg.compose.html.preview.psi.di.PsiModule
import com.makeevrserg.compose.html.preview.server.di.ServerModule
import com.makeevrserg.compose.html.preview.source.EditorTracker
import com.makeevrserg.compose.html.preview.source.SourceChangeTracker
import com.makeevrserg.compose.html.preview.source.ToolWindowVisibilityTracker
import com.makeevrserg.compose.html.preview.toolwindow.IntellijPreviewToolWindowPresenter

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
        contract = previewModule.previewStore,
        coroutineFeature = coreModule.backgroundCoroutineFeature
    )

    private val sourceChangeTracker = SourceChangeTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        contract = previewModule.previewStore
    )

    private val toolWindowVisibilityTracker = ToolWindowVisibilityTracker(
        projectDependencies = intellijCoreModule.projectDependencies,
        contract = previewModule.previewStore
    )

    /** Listeners are unregistered by [IntellijCoreModule.listenerDisposable], so nothing to do on disable. */
    val lifecycle: Lifecycle = LambdaLifecycle(
        onEnable = {
            editorTracker.start(intellijCoreModule.listenerDisposable)
            sourceChangeTracker.start(intellijCoreModule.listenerDisposable)
            toolWindowVisibilityTracker.start(intellijCoreModule.listenerDisposable)
        }
    )
}
