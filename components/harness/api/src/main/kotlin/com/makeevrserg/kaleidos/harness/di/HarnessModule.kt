package com.makeevrserg.kaleidos.harness.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.harness.DefaultPreviewHarness
import com.makeevrserg.kaleidos.harness.HarnessLayout
import com.makeevrserg.kaleidos.harness.HarnessPlanner
import com.makeevrserg.kaleidos.harness.HarnessWriter
import com.makeevrserg.kaleidos.harness.PreviewHarness
import com.makeevrserg.kaleidos.harness.ProjectPreviewSource
import com.makeevrserg.kaleidos.harness.SocketFreePortAllocator
import com.makeevrserg.kaleidos.harness.generator.HarnessNaming
import com.makeevrserg.kaleidos.harness.generator.HarnessSourceFactory
import com.makeevrserg.kaleidos.harness.generator.PreviewInitScriptFactory
import com.makeevrserg.kaleidos.harness.generator.PreviewPageFactory
import com.makeevrserg.kaleidos.harness.generator.PreviewPageSourceFactory
import com.makeevrserg.kaleidos.harness.generator.PreviewRegistryFactory
import com.makeevrserg.kaleidos.host.PreviewProjectStructureReader

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
