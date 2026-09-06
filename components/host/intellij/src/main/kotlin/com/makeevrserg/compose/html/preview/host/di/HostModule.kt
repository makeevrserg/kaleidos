package com.makeevrserg.compose.html.preview.host.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.host.GradleModuleCatalog
import com.makeevrserg.compose.html.preview.host.IntellijPreviewHostLocator
import com.makeevrserg.compose.html.preview.host.IntellijPreviewProjectStructureReader
import com.makeevrserg.compose.html.preview.host.ModuleDependencyGraphReader
import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostSelector
import com.makeevrserg.compose.html.preview.host.PreviewProjectStructureReader

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
