package com.makeevrserg.compose.html.preview.ui

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.makeevrserg.compose.html.preview.service.PreviewProjectService
import com.makeevrserg.compose.html.preview.ui.action.OpenDevToolsAction
import com.makeevrserg.compose.html.preview.ui.action.OpenInBrowserAction
import com.makeevrserg.compose.html.preview.ui.action.RefreshPreviewAction
import com.makeevrserg.compose.html.preview.ui.action.RestartDevServerAction
import com.makeevrserg.compose.html.preview.ui.action.StopDevServerAction
import kotlinx.coroutines.cancel

class PreviewToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val service = project.service<PreviewProjectService>()
        val contract = service.feature
        val browser = service.previewBrowserFactory.create()
        val panelScope = service.createMainCoroutineFeature()
        val panel = PreviewPanel(
            contract = contract,
            browser = browser,
            texts = service.previewStateTexts,
            coroutineFeature = panelScope
        )
        // No display name: the tool window shows only its own title, not the file name
        val content = ContentFactory.getInstance().createContent(panel.component, "", false)
        Disposer.register(toolWindow.disposable, browser)
        Disposer.register(toolWindow.disposable) { panelScope.cancel() }
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
                OpenDevToolsAction(panel, service.previewBrowserFactory.isJcefSupported)
            )
        )
    }
}
