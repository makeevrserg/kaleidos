package com.makeevrserg.kaleidos.preview.api.feature.usecase

import com.makeevrserg.kaleidos.harness.api.api.PreviewHarness
import com.makeevrserg.kaleidos.host.api.model.PreviewHost
import com.makeevrserg.kaleidos.preview.api.notification.api.PreviewNotifier
import com.makeevrserg.kaleidos.server.api.api.DevServerController
import com.makeevrserg.kaleidos.server.api.model.DevServerLaunchOptions

/**
 * Brings up the server that shows a host: the preview application is generated into the build
 * directories of the project first, and the run is then told where those generated sources are.
 *
 * Generation happens before every request, so a preview added, renamed or removed in the editor is on
 * the page as soon as the continuous build has recompiled the project.
 */
internal class LaunchPreviewServerUseCase(
    private val previewHarness: PreviewHarness,
    private val devServerController: DevServerController,
    private val previewNotifier: PreviewNotifier
) {

    private suspend fun install(host: PreviewHost): DevServerLaunchOptions? {
        return previewHarness.install(host).fold(
            onSuccess = { installation ->
                DevServerLaunchOptions(
                    initScriptPath = installation.initScriptPath,
                    devServerPort = installation.devServerPort
                )
            },
            onFailure = { error ->
                previewNotifier.error("Could not generate the preview page: ${error.message ?: error}")
                null
            }
        )
    }

    suspend operator fun invoke(
        host: PreviewHost,
        retryAfterFailure: Boolean,
        isRestart: Boolean = false
    ) {
        val options = install(host) ?: return
        if (isRestart) {
            devServerController.restart(host, options)
        } else {
            devServerController.requestRunning(
                host,
                options,
                retryAfterFailure
            )
        }
    }
}
