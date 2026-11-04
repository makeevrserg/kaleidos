package com.makeevrserg.kaleidos.preview.intellij.toolwindow.component

import com.makeevrserg.kaleidos.core.di.ProjectDependencies
import com.makeevrserg.kaleidos.preview.api.feature.api.PreviewToolWindowPresenter
import com.makeevrserg.kaleidos.preview.intellij.toolwindow.model.PreviewToolWindowIds

/** Must be called on the event dispatch thread. */
internal class IntellijPreviewToolWindowPresenter(
    private val projectDependencies: ProjectDependencies
) : PreviewToolWindowPresenter {
    override fun show() {
        projectDependencies.toolWindowManager.getToolWindow(PreviewToolWindowIds.ID)?.show()
    }
}
