package com.makeevrserg.compose.html.preview.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.feature.renderablePreviews

class RestartDevServerAction(
    private val contract: PreviewStore
) : AnAction(
    "Restart Dev Server",
    "Stop the dev server of this module, whoever started it, and start it again",
    AllIcons.Actions.Restart
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = contract.state.value.target?.renderablePreviews?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) = contract.onRestartServer()
}
