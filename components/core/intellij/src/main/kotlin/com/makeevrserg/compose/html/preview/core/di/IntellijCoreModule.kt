package com.makeevrserg.compose.html.preview.core.di

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.makeevrserg.compose.html.preview.core.lifecycle.LambdaLifecycle
import com.makeevrserg.compose.html.preview.core.lifecycle.Lifecycle
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies

/**
 * IDE services of one project.
 *
 * @param parentDisposable owner of every listener the components register; the project service
 */
class IntellijCoreModule(
    val project: Project,
    parentDisposable: Disposable
) {
    val projectDependencies = ProjectDependencies(project)

    /** Parent for message bus connections and listeners; disposed when the components are disabled. */
    val listenerDisposable: Disposable = Disposer.newDisposable(parentDisposable, "Compose HTML Preview listeners")

    val lifecycle: Lifecycle = LambdaLifecycle(
        onDisable = { Disposer.dispose(listenerDisposable) }
    )
}
