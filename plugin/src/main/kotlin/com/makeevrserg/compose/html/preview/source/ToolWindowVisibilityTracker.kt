package com.makeevrserg.compose.html.preview.source

import com.intellij.openapi.Disposable
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.ui.PreviewToolWindowIds

/**
 * Reports whether the preview tool window is visible, so nothing is rendered and no dev server is
 * started while the user cannot see the result.
 */
class ToolWindowVisibilityTracker(
    private val projectDependencies: ProjectDependencies,
    private val contract: PreviewStore
) : ToolWindowManagerListener {

    private fun publish() {
        val toolWindow = projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)
        contract.onToolWindowVisibilityChanged(toolWindow?.isVisible == true)
    }

    override fun stateChanged(toolWindowManager: ToolWindowManager) = publish()

    fun start(parentDisposable: Disposable) {
        projectDependencies.messageBus.connect(parentDisposable).subscribe(ToolWindowManagerListener.TOPIC, this)
        publish()
    }
}
