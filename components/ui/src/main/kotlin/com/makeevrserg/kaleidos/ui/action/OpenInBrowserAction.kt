package com.makeevrserg.kaleidos.ui.action

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.kaleidos.feature.PreviewStore

class OpenInBrowserAction(
    private val contract: PreviewStore
) : AnAction(
    "Open in Browser",
    "Open the current preview page in the system browser",
    AllIcons.Ide.External_link_arrow
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = contract.state.value.previewUrl != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val previewUrl = contract.state.value.previewUrl ?: return
        BrowserUtil.browse(previewUrl)
    }
}
