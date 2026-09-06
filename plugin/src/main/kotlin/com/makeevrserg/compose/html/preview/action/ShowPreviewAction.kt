package com.makeevrserg.compose.html.preview.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.makeevrserg.compose.html.preview.feature.PreviewFunction
import com.makeevrserg.compose.html.preview.service.PreviewProjectService
import com.makeevrserg.compose.html.preview.ui.PreviewIcons

/**
 * Gutter action. The preview already follows the editor; the icon opens the tool window and scrolls
 * the page to this particular function.
 */
class ShowPreviewAction(
    private val previewFunction: PreviewFunction
) : AnAction(
    "Show Preview",
    "Open the Compose HTML Preview tool window and scroll to ${previewFunction.name}",
    PreviewIcons.Gutter
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<PreviewProjectService>().feature.onFocusPreview(previewFunction)
    }
}
