package com.makeevrserg.kaleidos.feature.fake

import com.makeevrserg.kaleidos.harness.api.PreviewHarness
import com.makeevrserg.kaleidos.harness.model.HarnessInstallation
import com.makeevrserg.kaleidos.host.model.PreviewHost

class FakePreviewHarness : PreviewHarness {
    val installedHosts = mutableListOf<PreviewHost>()

    var failure: Throwable? = null

    override suspend fun install(host: PreviewHost): Result<HarnessInstallation> {
        installedHosts += host
        val error = failure
        if (error != null) return Result.failure(error)
        return Result.success(
            HarnessInstallation(initScriptPath = "$INIT_SCRIPT_DIRECTORY${host.gradlePath}", devServerPort = PORT)
        )
    }

    companion object {
        const val INIT_SCRIPT_DIRECTORY = "/generated"
        const val PORT = 8301
    }
}
