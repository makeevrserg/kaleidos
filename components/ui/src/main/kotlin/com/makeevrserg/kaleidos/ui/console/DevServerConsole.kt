package com.makeevrserg.kaleidos.ui.console

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.makeevrserg.kaleidos.server.DevServerLog
import com.makeevrserg.kaleidos.server.DevServerLogEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class DevServerConsole(
    project: Project,
    private val log: DevServerLog,
    private val texts: DevServerConsoleTexts,
    coroutineScope: CoroutineScope
) : Disposable {
    private val console: ConsoleView = TextConsoleBuilderFactory.getInstance()
        .createBuilder(project)
        .apply { setViewer(true) }
        .console

    val component: JComponent = createComponent()

    private val collection: Job = coroutineScope.launch { log.events.collect(::show) }

    private fun toolbar(consoleComponent: JComponent): JComponent {
        val actions = DefaultActionGroup(*console.createConsoleActions())
        val toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, actions, false)
        toolbar.targetComponent = consoleComponent
        return toolbar.component
    }

    private fun createComponent(): JComponent {
        val consoleComponent = console.component
        val panel = JPanel(BorderLayout())
        panel.add(consoleComponent, BorderLayout.CENTER)
        panel.add(toolbar(consoleComponent), BorderLayout.WEST)
        return panel
    }

    private fun show(event: DevServerLogEvent) {
        when (event) {
            is DevServerLogEvent.RunStarted -> {
                console.clear()
                console.print(texts.runStarted(event.config), ConsoleViewContentType.SYSTEM_OUTPUT)
            }
            is DevServerLogEvent.Output -> console.print(event.text, ConsoleViewContentType.NORMAL_OUTPUT)
            is DevServerLogEvent.Adopted -> console.print(
                texts.adopted(event.baseUrl),
                ConsoleViewContentType.SYSTEM_OUTPUT
            )
        }
    }

    override fun dispose() {
        collection.cancel()
        Disposer.dispose(console)
    }

    private companion object {
        const val TOOLBAR_PLACE = "KaleidosDevServerConsole"
    }
}
