package com.makeevrserg.compose.html.preview.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.compose.html.preview.feature.PreviewStore

class RestartDevServerAction(
    private val contract: PreviewStore
) : AnAction(
    "Restart Dev Server",
    "Stop the dev server started by the plugin and start it again",
    AllIcons.Actions.Restart
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = contract.state.value.target?.previews?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) = contract.onRestartServer()
}
