package com.makeevrserg.compose.html.preview.toolwindow

import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.feature.PreviewToolWindowPresenter

/** Must be called on the event dispatch thread. */
class IntellijPreviewToolWindowPresenter(
    private val projectDependencies: ProjectDependencies
) : PreviewToolWindowPresenter {
    override fun show() {
        projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)?.show()
    }
}
