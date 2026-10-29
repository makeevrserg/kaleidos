package com.makeevrserg.kaleidos.host.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.host.api.PreviewHostLocator
import com.makeevrserg.kaleidos.host.api.PreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.gradle.GradleModuleCatalog
import com.makeevrserg.kaleidos.host.gradle.IntellijPreviewHostLocator
import com.makeevrserg.kaleidos.host.gradle.IntellijPreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.gradle.ModuleDependencyGraphReader
import com.makeevrserg.kaleidos.host.usecase.SelectPreviewHostUseCase

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
