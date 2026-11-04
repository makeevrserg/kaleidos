package com.makeevrserg.kaleidos.server.intellij.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.server.api.api.GradleTaskRunner
import com.makeevrserg.kaleidos.server.intellij.gradle.ExternalSystemGradleTaskRunner
import com.makeevrserg.kaleidos.server.intellij.gradle.RunOutputForwarder

class IntellijServerModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    val gradleTaskRunner: GradleTaskRunner = ExternalSystemGradleTaskRunner(
        projectDependencies = intellijCoreModule.projectDependencies,
        mainContext = coreModule.dispatchers.main,
        backgroundContext = coreModule.dispatchers.default,
        runOutputForwarder = ::RunOutputForwarder
    )
}
