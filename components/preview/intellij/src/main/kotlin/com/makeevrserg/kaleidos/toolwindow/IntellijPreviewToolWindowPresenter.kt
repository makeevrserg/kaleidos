package com.makeevrserg.kaleidos.toolwindow

import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.PreviewToolWindowPresenter

/** Must be called on the event dispatch thread. */
class IntellijPreviewToolWindowPresenter(
    private val projectDependencies: ProjectDependencies
) : PreviewToolWindowPresenter {
    override fun show() {
        projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)?.show()
    }
}
