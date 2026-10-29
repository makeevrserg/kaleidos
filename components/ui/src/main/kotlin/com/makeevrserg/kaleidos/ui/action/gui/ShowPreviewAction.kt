package com.makeevrserg.kaleidos.ui.action.gui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.kaleidos.feature.api.PreviewStore
import com.makeevrserg.kaleidos.feature.model.PreviewFunction
import com.makeevrserg.kaleidos.ui.theme.PreviewIcons

/**
 * Gutter action. The preview already follows the editor; the icon opens the tool window and scrolls
 * the page to this particular function.
 */
internal class ShowPreviewAction(
    private val previewStore: PreviewStore,
    private val previewFunction: PreviewFunction
) : AnAction(
    "Show Preview",
    "Open the Kaleidos tool window and scroll to ${previewFunction.name}",
    PreviewIcons.Gutter
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        previewStore.onFocusPreview(previewFunction)
    }
}
