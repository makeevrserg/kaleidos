package com.makeevrserg.kaleidos.feature

import com.makeevrserg.kaleidos.harness.PreviewHarness
import com.makeevrserg.kaleidos.host.PreviewHost
import com.makeevrserg.kaleidos.notification.PreviewNotifier
import com.makeevrserg.kaleidos.server.DevServerController
import com.makeevrserg.kaleidos.server.DevServerLaunchOptions

/**
 * Brings up the server that shows a host: the preview application is generated into the build
 * directories of the project first, and the run is then told where those generated sources are.
 *
 * Generation happens before every request, so a preview added, renamed or removed in the editor is on
 * the page as soon as the continuous build has recompiled the project.
 */
class PreviewServerLauncher(
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

    suspend fun requestRunning(host: PreviewHost, retryAfterFailure: Boolean) {
        val options = install(host) ?: return
        devServerController.requestRunning(host, options, retryAfterFailure)
    }

    suspend fun restart(host: PreviewHost) {
        val options = install(host) ?: return
        devServerController.restart(host, options)
    }
}
