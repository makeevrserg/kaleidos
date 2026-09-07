package com.makeevrserg.kaleidos.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.kaleidos.ui.PreviewPanel

class OpenDevToolsAction(
    private val previewPanel: PreviewPanel,
    private val isDevToolsSupported: Boolean
) : AnAction("Open DevTools", "Open Chromium DevTools for the preview page", AllIcons.Actions.StartDebugger) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isDevToolsSupported
    }

    override fun actionPerformed(e: AnActionEvent) = previewPanel.openDevTools()
}
