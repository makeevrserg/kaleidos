package com.makeevrserg.kaleidos.core.di

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/** IDE services of one project. */
class IntellijCoreModule(
    val project: Project
) {
    val mainContext: CoroutineContext = Dispatchers.EDT

    val projectDependencies = ProjectDependencies(project)
}
