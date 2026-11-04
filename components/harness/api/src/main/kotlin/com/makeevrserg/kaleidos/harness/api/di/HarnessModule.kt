package com.makeevrserg.kaleidos.harness.api.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.harness.api.api.PreviewHarness
import com.makeevrserg.kaleidos.harness.api.api.ProjectPreviewSource
import com.makeevrserg.kaleidos.harness.api.memory.SocketFreePortAllocator
import com.makeevrserg.kaleidos.harness.api.service.DefaultHarnessWriter
import com.makeevrserg.kaleidos.harness.api.service.DefaultPreviewHarness
import com.makeevrserg.kaleidos.harness.api.usecase.PlanHarnessUseCase
import com.makeevrserg.kaleidos.host.api.api.PreviewProjectStructureReader

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
