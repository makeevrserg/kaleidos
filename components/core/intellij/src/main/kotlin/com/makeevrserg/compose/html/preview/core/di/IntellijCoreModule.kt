package com.makeevrserg.compose.html.preview.core.di

import com.intellij.openapi.project.Project
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies

/** IDE services of one project. */
class IntellijCoreModule(
    val project: Project
) {
    val projectDependencies = ProjectDependencies(project)
}
