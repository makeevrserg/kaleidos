package com.makeevrserg.kaleidos.ui.action.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.makeevrserg.kaleidos.ui.console.component.PreviewConsoleSplitter

internal class ToggleDevServerConsoleAction(
    private val splitter: PreviewConsoleSplitter
) : DumbAwareToggleAction(
    "Dev Server Console",
    "Show the output of the dev server and its Gradle task under the page",
    AllIcons.Debugger.Console
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun isSelected(e: AnActionEvent): Boolean = splitter.isConsoleVisible

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        splitter.isConsoleVisible = state
    }
}
