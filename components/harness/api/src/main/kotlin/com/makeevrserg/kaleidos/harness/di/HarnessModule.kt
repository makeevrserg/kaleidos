package com.makeevrserg.kaleidos.harness.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.harness.api.PreviewHarness
import com.makeevrserg.kaleidos.harness.api.ProjectPreviewSource
import com.makeevrserg.kaleidos.harness.memory.SocketFreePortAllocator
import com.makeevrserg.kaleidos.harness.service.DefaultHarnessWriter
import com.makeevrserg.kaleidos.harness.service.DefaultPreviewHarness
import com.makeevrserg.kaleidos.harness.usecase.PlanHarnessUseCase
import com.makeevrserg.kaleidos.host.api.PreviewProjectStructureReader

class HarnessModule(
    coreModule: CoreModule,
    projectPreviewSource: ProjectPreviewSource,
    structureReader: PreviewProjectStructureReader
) {
    val previewHarness: PreviewHarness = DefaultPreviewHarness(
        structureReader = structureReader,
        planner = PlanHarnessUseCase(
            projectPreviewSource = projectPreviewSource,
            portAllocator = SocketFreePortAllocator(DEV_SERVER_PORTS)
        ),
        writer = DefaultHarnessWriter(ioContext = coreModule.dispatchers.io)
    )

    private companion object {
        /** Away from the ports applications of a project usually take, so a preview never collides with one. */
        val DEV_SERVER_PORTS = 8300..8399
    }
}
