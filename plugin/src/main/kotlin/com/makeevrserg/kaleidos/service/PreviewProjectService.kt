package com.makeevrserg.kaleidos.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.makeevrserg.kaleidos.di.RootModule
import kotlinx.coroutines.CoroutineScope

/**
 * Holds the object graph of the plugin for one project. IntelliJ instantiates it lazily and cancels
 * [coroutineScope] when the project closes; the graph itself is wired in [RootModule]. Entry points
 * created by the platform without constructors ([com.intellij.openapi.actionSystem.AnAction],
 * tool window factories, line marker contributors) reach the graph through this service.
 */
@Service(Service.Level.PROJECT)
class PreviewProjectService(project: Project, coroutineScope: CoroutineScope) : Disposable {
    val rootModule = RootModule(
        project = project,
        coroutineScope = coroutineScope
    )

    init {
        rootModule.lifecycle.onEnable()
    }

    override fun dispose() = rootModule.lifecycle.onDisable()
}
