package com.makeevrserg.compose.html.preview.harness.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.harness.DefaultPreviewHarness
import com.makeevrserg.compose.html.preview.harness.HarnessLayout
import com.makeevrserg.compose.html.preview.harness.HarnessPlanner
import com.makeevrserg.compose.html.preview.harness.HarnessWriter
import com.makeevrserg.compose.html.preview.harness.PreviewHarness
import com.makeevrserg.compose.html.preview.harness.ProjectPreviewSource
import com.makeevrserg.compose.html.preview.harness.SocketFreePortAllocator
import com.makeevrserg.compose.html.preview.harness.generator.HarnessNaming
import com.makeevrserg.compose.html.preview.harness.generator.HarnessSourceFactory
import com.makeevrserg.compose.html.preview.harness.generator.PreviewInitScriptFactory
import com.makeevrserg.compose.html.preview.harness.generator.PreviewPageFactory
import com.makeevrserg.compose.html.preview.harness.generator.PreviewPageSourceFactory
import com.makeevrserg.compose.html.preview.harness.generator.PreviewRegistryFactory
import com.makeevrserg.compose.html.preview.host.PreviewProjectStructureReader

class HarnessModule(
    coreModule: CoreModule,
    projectPreviewSource: ProjectPreviewSource,
    structureReader: PreviewProjectStructureReader
) {
    private val layout = HarnessLayout()

    private val naming = HarnessNaming()

    val previewHarness: PreviewHarness = DefaultPreviewHarness(
        structureReader = structureReader,
        planner = HarnessPlanner(
            projectPreviewSource = projectPreviewSource,
            portAllocator = SocketFreePortAllocator(DEV_SERVER_PORTS)
        ),
        sourceFactory = HarnessSourceFactory(
            registryFactory = PreviewRegistryFactory(naming),
            pageFactory = PreviewPageFactory(naming, PreviewPageSourceFactory(naming)),
            initScriptFactory = PreviewInitScriptFactory(layout)
        ),
        writer = HarnessWriter(ioContext = coreModule.dispatchers.io),
        layout = layout
    )

    private companion object {
        /** Away from the ports applications of a project usually take, so a preview never collides with one. */
        val DEV_SERVER_PORTS = 8300..8399
    }
}
