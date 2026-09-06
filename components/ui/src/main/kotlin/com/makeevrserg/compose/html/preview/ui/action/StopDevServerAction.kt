package com.makeevrserg.compose.html.preview.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.makeevrserg.compose.html.preview.feature.PreviewStore
import com.makeevrserg.compose.html.preview.server.DevServerState

class StopDevServerAction(
    private val contract: PreviewStore
) : AnAction("Stop Dev Server", "Stop the dev server started by the plugin", AllIcons.Actions.Suspend) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = contract.state.value.serverState !is DevServerState.Stopped
    }

    override fun actionPerformed(e: AnActionEvent) = contract.onStopServer()
}
