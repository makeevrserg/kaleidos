package com.makeevrserg.kaleidos.harness

import com.makeevrserg.kaleidos.harness.generator.HarnessSourceFactory
import com.makeevrserg.kaleidos.host.PreviewHost
import com.makeevrserg.kaleidos.host.PreviewProjectStructureReader

/**
 * Writes the preview application before every launch. Generation is cheap and idempotent: unchanged
 * files are not touched, so a run that is already going keeps its incremental build.
 */
class DefaultPreviewHarness(
    private val structureReader: PreviewProjectStructureReader,
    private val planner: HarnessPlanner,
    private val sourceFactory: HarnessSourceFactory,
    private val writer: HarnessWriter,
    private val layout: HarnessLayout
) : PreviewHarness {

    override suspend fun install(host: PreviewHost): Result<HarnessInstallation> = runCatching {
        val plan = planner.plan(host, structureReader.read())
        writer.write(sourceFactory.create(plan), layout)
        HarnessInstallation(
            initScriptPath = layout.initScript(host.directory).toString(),
            devServerPort = plan.host.devServerPort
        )
    }
}
