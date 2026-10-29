package com.makeevrserg.kaleidos.core.di

import com.intellij.openapi.project.Project
import com.makeevrserg.kaleidos.core.coroutine.IntellijDispatchers
import com.makeevrserg.kaleidos.core.coroutine.api.PreviewDispatchers
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies

/** IDE services of one project. */
class IntellijCoreModule(
    val project: Project
) {
    val dispatchers: PreviewDispatchers = IntellijDispatchers()

    val projectDependencies = ProjectDependencies(project)
}
