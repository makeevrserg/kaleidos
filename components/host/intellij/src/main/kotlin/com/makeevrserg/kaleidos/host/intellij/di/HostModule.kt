package com.makeevrserg.kaleidos.host.intellij.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.host.api.api.PreviewHostLocator
import com.makeevrserg.kaleidos.host.api.api.PreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.api.usecase.SelectPreviewHostUseCase
import com.makeevrserg.kaleidos.host.intellij.gradle.GradleModuleCatalog
import com.makeevrserg.kaleidos.host.intellij.gradle.IntellijPreviewHostLocator
import com.makeevrserg.kaleidos.host.intellij.gradle.IntellijPreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.intellij.gradle.ModuleDependencyGraphReader

class HostModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    private val graphReader = ModuleDependencyGraphReader(intellijCoreModule.projectDependencies)

    val projectStructureReader: PreviewProjectStructureReader = IntellijPreviewProjectStructureReader(
        gradleModuleCatalog = GradleModuleCatalog(intellijCoreModule.projectDependencies),
        graphReader = graphReader,
        backgroundContext = coreModule.dispatchers.default
    )

    val previewHostLocator: PreviewHostLocator = IntellijPreviewHostLocator(
        structureReader = projectStructureReader,
        graphReader = graphReader,
        selector = SelectPreviewHostUseCase(),
        backgroundContext = coreModule.dispatchers.default
    )
}
