package com.makeevrserg.kaleidos

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.makeevrserg.kaleidos.di.PreviewProjectService

@Suppress("RolelessClassName")
internal class PreviewToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val rootModule = project.service<PreviewProjectService>().rootModule
        val uiModule = rootModule.uiModule
        val browser = uiModule.previewBrowser
        val panel = uiModule.createPreviewPanel(browser)
        val console = uiModule.createDevServerConsole()
        val splitter = uiModule.createPreviewConsoleSplitter(page = panel.component, console = console.component)
        // No display name: the tool window shows only its own title, not the file name
        val content = ContentFactory.getInstance().createContent(
            splitter.component,
            "",
            false
        )
        Disposer.register(toolWindow.disposable, browser)
        Disposer.register(toolWindow.disposable, console)
        toolWindow.contentManager.addContent(content)

        toolWindow.setTitleActions(uiModule.createTitleActions(panel, splitter))
        toolWindow.setAdditionalGearActions(uiModule.createGearActions(panel))
    }
}
