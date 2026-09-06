package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * One dev server as a cold flow: collecting [launch] adopts a server that already answers or starts the
 * Gradle task of the module, and cancelling the collector stops what was started. The run belongs to the
 * collecting coroutine, so a closed project or a switch to another module can never leave it behind.
 *
 * Only runs started here are stopped. An adopted server, from a terminal or an earlier IDE session, is
 * left untouched.
 */
class DevServerLauncher(
    private val gradleTaskRunner: GradleTaskRunner,
    private val healthCheck: DevServerHealthCheck,
    private val originResolver: DevServerOriginResolver,
    private val urlDetector: DevServerUrlDetector,
    private val startupTimeout: Duration,
    private val pollInterval: Duration
) {
    private suspend fun startRun(host: PreviewHost, config: DevServerLaunchConfig): DevServerRunSignals {
        val signals = DevServerRunSignals(
            announcedBaseUrl = CompletableDeferred(),
            exit = CompletableDeferred()
        )
        gradleTaskRunner.start(
            config = config,
            executionName = DevServerRunNames.devServer(host),
            listener = DevServerRunListener(
                urlDetector = urlDetector,
                onBaseUrlAnnounced = { baseUrl -> signals.announcedBaseUrl.complete(baseUrl) },
                onExited = { isSuccess -> signals.exit.complete(isSuccess) }
            )
        )
        return signals
    }

    /** Terminates the run and, for servers that outlive it such as Kobweb's, runs the stop task. */
    private suspend fun stopRun(host: PreviewHost) {
        gradleTaskRunner.stop(DevServerRunNames.devServer(host))
        val stopTask = host.kind.stopTask ?: return
        gradleTaskRunner.start(
            config = DevServerLaunchConfig.forHost(host, taskName = stopTask, arguments = ""),
            executionName = DevServerRunNames.STOP_TASK,
            listener = SilentProcessListener
        )
    }

    /** The origin the run announced or, before the announcement, the one expected for the module. */
    private suspend fun candidateBaseUrl(run: DevServerRunSignals, expectedBaseUrl: String?): String? {
        return if (run.announcedBaseUrl.isCompleted) run.announcedBaseUrl.await() else expectedBaseUrl
    }

    /** Polls until the server answers. Null when the run exits or the startup timeout passes first. */
    private suspend fun awaitAlive(run: DevServerRunSignals, expectedBaseUrl: String?): String? {
        return withTimeoutOrNull(startupTimeout) {
            var aliveBaseUrl: String? = null
            while (aliveBaseUrl == null && !run.exit.isCompleted) {
                val candidate = candidateBaseUrl(run, expectedBaseUrl)
                val isAlive = candidate != null && healthCheck.isAlive(candidate)
                if (isAlive) aliveBaseUrl = candidate else delay(pollInterval)
            }
            aliveBaseUrl
        }
    }

    /**
     * A successful exit does not mean the server is gone: Kobweb leaves its server running after
     * `kobwebStart` returns. The port decides. Returns only once nothing of the run is left to stop.
     */
    private suspend fun ProducerScope<DevServerState>.serveAfterExit(
        host: PreviewHost,
        run: DevServerRunSignals,
        expectedBaseUrl: String?,
        qualifiedTaskName: String
    ) {
        val isSuccess = run.exit.await()
        if (!isSuccess) {
            send(DevServerState.Failed("Gradle task '$qualifiedTaskName' failed. See the Run tool window for details."))
            return
        }
        val survivingBaseUrl = candidateBaseUrl(run, expectedBaseUrl)
            ?.takeIf { candidate -> healthCheck.isAlive(candidate) }
        if (survivingBaseUrl == null) {
            send(DevServerState.Stopped)
            return
        }
        send(DevServerState.Running(host, survivingBaseUrl))
        awaitCancellation()
    }

    /** Suspends while the run is alive; returns normally only when the run has exited on its own. */
    private suspend fun ProducerScope<DevServerState>.serve(
        host: PreviewHost,
        run: DevServerRunSignals,
        expectedBaseUrl: String?,
        config: DevServerLaunchConfig
    ) {
        val aliveBaseUrl = awaitAlive(run, expectedBaseUrl)
        when {
            aliveBaseUrl != null -> send(DevServerState.Running(host, aliveBaseUrl))
            !run.exit.isCompleted -> send(
                DevServerState.Failed(
                    "Dev server of ${host.displayName} did not answer within $startupTimeout. " +
                        "See the Run tool window for details."
                )
            )
        }
        serveAfterExit(host, run, expectedBaseUrl, config.qualifiedTaskName)
    }

    /**
     * The run is stopped on every exit of this function except its own: cancellation of the collector,
     * and any failure of the flow itself, must not leave a Gradle build behind.
     */
    private suspend fun ProducerScope<DevServerState>.launchAndServe(host: PreviewHost) {
        send(DevServerState.Starting(host))
        val config = DevServerLaunchConfig.forHost(
            host = host,
            taskName = host.kind.startTask,
            arguments = host.kind.arguments
        )
        val expectedBaseUrl = originResolver.expected(host)
        val run = startRun(host, config)
        var isRunOwned = true
        try {
            serve(host, run, expectedBaseUrl, config)
            isRunOwned = false
        } finally {
            if (isRunOwned) withContext(NonCancellable) { stopRun(host) }
        }
    }

    /**
     * Completes on its own only when the run exits and leaves no server behind; otherwise it stays
     * active until cancelled, and cancellation stops the run.
     */
    fun launch(host: PreviewHost): Flow<DevServerState> = channelFlow {
        val adoptedBaseUrl = originResolver.findAlive(host)
        if (adoptedBaseUrl != null) {
            send(DevServerState.Running(host, adoptedBaseUrl))
            awaitCancellation()
        }
        launchAndServe(host)
    }
}
