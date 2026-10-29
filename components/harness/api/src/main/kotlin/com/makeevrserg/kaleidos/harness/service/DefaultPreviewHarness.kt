package com.makeevrserg.kaleidos.harness.service

import com.makeevrserg.kaleidos.core.coroutine.util.propagateCancellationException
import com.makeevrserg.kaleidos.harness.api.HarnessWriter
import com.makeevrserg.kaleidos.harness.api.PreviewHarness
import com.makeevrserg.kaleidos.harness.generator.util.generateHarnessSources
import com.makeevrserg.kaleidos.harness.model.HarnessInstallation
import com.makeevrserg.kaleidos.harness.usecase.PlanHarnessUseCase
import com.makeevrserg.kaleidos.harness.util.harnessInitScript
import com.makeevrserg.kaleidos.host.api.PreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.model.PreviewHost

/**
 * Writes the preview application before every launch. Generation is cheap and idempotent: unchanged
 * files are not touched, so a run that is already going keeps its incremental build.
 */
internal class DefaultPreviewHarness(
    private val structureReader: PreviewProjectStructureReader,
    private val planner: PlanHarnessUseCase,
    private val writer: HarnessWriter
) : PreviewHarness {

    override suspend fun install(host: PreviewHost): Result<HarnessInstallation> {
        return runCatching {
            val plan = planner.invoke(host, structureReader.read())
            writer.write(plan.generateHarnessSources())
            HarnessInstallation(
                initScriptPath = harnessInitScript(host.directory).toString(),
                devServerPort = plan.host.devServerPort
            )
        }.propagateCancellationException()
    }
}
