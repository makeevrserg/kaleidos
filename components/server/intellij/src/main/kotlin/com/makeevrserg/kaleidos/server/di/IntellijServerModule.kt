package com.makeevrserg.kaleidos.server.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.server.ExternalSystemGradleTaskRunner
import com.makeevrserg.kaleidos.server.GradleTaskRunner

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
