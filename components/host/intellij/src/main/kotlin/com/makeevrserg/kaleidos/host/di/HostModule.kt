package com.makeevrserg.kaleidos.host.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.host.GradleModuleCatalog
import com.makeevrserg.kaleidos.host.IntellijPreviewHostLocator
import com.makeevrserg.kaleidos.host.IntellijPreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.ModuleDependencyGraphReader
import com.makeevrserg.kaleidos.host.PreviewHostLocator
import com.makeevrserg.kaleidos.host.PreviewHostSelector
import com.makeevrserg.kaleidos.host.PreviewProjectStructureReader

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
        selector = PreviewHostSelector(),
        backgroundContext = coreModule.dispatchers.default
    )
}
