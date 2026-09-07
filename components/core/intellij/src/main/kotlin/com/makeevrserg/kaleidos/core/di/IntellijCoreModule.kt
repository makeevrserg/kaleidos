package com.makeevrserg.kaleidos.core.di

import com.intellij.openapi.project.Project
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies

/** IDE services of one project. */
class IntellijCoreModule(
    val project: Project
) {
    val projectDependencies = ProjectDependencies(project)
}
