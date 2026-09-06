package com.makeevrserg.compose.html.preview.server.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.server.ExternalSystemGradleTaskRunner
import com.makeevrserg.compose.html.preview.server.GradleTaskRunner

class IntellijServerModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    val gradleTaskRunner: GradleTaskRunner = ExternalSystemGradleTaskRunner(
        projectDependencies = intellijCoreModule.projectDependencies,
        mainContext = coreModule.dispatchers.main,
        backgroundContext = coreModule.dispatchers.default
    )
}
