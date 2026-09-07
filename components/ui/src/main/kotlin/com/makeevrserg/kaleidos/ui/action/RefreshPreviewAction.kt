package com.makeevrserg.kaleidos.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.kaleidos.feature.PreviewStore
import com.makeevrserg.kaleidos.feature.renderablePreviews
import com.makeevrserg.kaleidos.ui.PreviewPanel

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
