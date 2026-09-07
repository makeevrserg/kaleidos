package com.makeevrserg.kaleidos.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.makeevrserg.kaleidos.feature.PreviewFunction
import com.makeevrserg.kaleidos.service.PreviewProjectService
import com.makeevrserg.kaleidos.ui.PreviewIcons

/**
 * Gutter action. The preview already follows the editor; the icon opens the tool window and scrolls
 * the page to this particular function.
 */
class ShowPreviewAction(
    private val previewFunction: PreviewFunction
) : AnAction(
    "Show Preview",
    "Open the Kaleidos tool window and scroll to ${previewFunction.name}",
    PreviewIcons.Gutter
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<PreviewProjectService>().rootModule.previewStore.onFocusPreview(previewFunction)
    }
}
