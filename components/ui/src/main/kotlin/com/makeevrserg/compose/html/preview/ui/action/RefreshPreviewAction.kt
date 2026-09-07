package com.makeevrserg.compose.html.preview.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.renderablePreviews
import com.makeevrserg.compose.html.preview.ui.PreviewPanel

class RefreshPreviewAction(
    private val contract: PreviewStore,
    private val previewPanel: PreviewPanel
) : AnAction(
    "Refresh Preview",
    "Reload the page and reconnect to the dev server",
    AllIcons.Actions.Refresh
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = contract.state.value.target?.renderablePreviews?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        previewPanel.reload()
        contract.onReconnect()
    }
}
