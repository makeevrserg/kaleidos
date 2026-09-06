package com.makeevrserg.compose.html.preview.host.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.host.GradleModuleCatalog
import com.makeevrserg.compose.html.preview.host.IntellijPreviewHostLocator
import com.makeevrserg.compose.html.preview.host.ModuleDependencyGraphReader
import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostSelector

class HostModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    val previewHostLocator: PreviewHostLocator = IntellijPreviewHostLocator(
        gradleModuleCatalog = GradleModuleCatalog(intellijCoreModule.projectDependencies),
        graphReader = ModuleDependencyGraphReader(intellijCoreModule.projectDependencies),
        selector = PreviewHostSelector(),
        backgroundContext = coreModule.dispatchers.default
    )
}
