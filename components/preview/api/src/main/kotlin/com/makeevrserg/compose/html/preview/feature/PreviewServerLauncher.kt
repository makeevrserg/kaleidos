package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.harness.PreviewHarness
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.notification.PreviewNotifier
import com.makeevrserg.compose.html.preview.server.DevServerController
import com.makeevrserg.compose.html.preview.server.DevServerLaunchOptions

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
