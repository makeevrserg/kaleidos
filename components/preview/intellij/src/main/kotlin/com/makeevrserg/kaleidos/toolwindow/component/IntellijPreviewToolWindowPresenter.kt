package com.makeevrserg.kaleidos.toolwindow.component

import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.api.PreviewToolWindowPresenter
import com.makeevrserg.kaleidos.toolwindow.model.PreviewToolWindowIds

/** Must be called on the event dispatch thread. */
internal class IntellijPreviewToolWindowPresenter(
    private val projectDependencies: ProjectDependencies
) : PreviewToolWindowPresenter {
    override fun show() {
        projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)?.show()
    }
}
