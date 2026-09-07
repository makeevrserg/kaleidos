package com.makeevrserg.kaleidos.ui

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.makeevrserg.kaleidos.service.PreviewProjectService
import com.makeevrserg.kaleidos.ui.action.OpenDevToolsAction
import com.makeevrserg.kaleidos.ui.action.OpenInBrowserAction
import com.makeevrserg.kaleidos.ui.action.RefreshPreviewAction
import com.makeevrserg.kaleidos.ui.action.RestartDevServerAction
import com.makeevrserg.kaleidos.ui.action.StopDevServerAction

class PreviewToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val rootModule = project.service<PreviewProjectService>().rootModule
        val contract = rootModule.previewStore
        val uiModule = rootModule.uiModule
        val browser = uiModule.previewBrowserFactory.create()
        val panel = uiModule.createPreviewPanel(browser)
        // No display name: the tool window shows only its own title, not the file name
        val content = ContentFactory.getInstance().createContent(panel.component, "", false)
        Disposer.register(toolWindow.disposable, browser)
        toolWindow.contentManager.addContent(content)

        toolWindow.setTitleActions(
            listOf(
                RefreshPreviewAction(contract, panel),
                OpenInBrowserAction(contract)
            )
        )
        toolWindow.setAdditionalGearActions(
            DefaultActionGroup(
                RestartDevServerAction(contract),
                StopDevServerAction(contract),
                OpenDevToolsAction(panel, uiModule.previewBrowserFactory.isJcefSupported)
            )
        )
    }
}
