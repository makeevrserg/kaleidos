package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.core.CoroutineFeature
import com.makeevrserg.compose.html.preview.host.PreviewHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration

/**
 * Nothing is configured: the task follows the kind of the module and the origin comes from the
 * output of the run or from the Kobweb configuration file. A server that is already up, from an
 * earlier IDE session or a terminal, is adopted without launching Gradle.
 *
 * Only state transitions are serialised by the mutex; waiting for the server happens outside of it,
 * so [stop] and [restart] stay responsive while a launch is still in progress.
 */
class DefaultDevServerController(
    private val healthCheck: DevServerHealthCheck,
    private val gradleTaskRunner: GradleTaskRunner,
    private val originResolver: DevServerOriginResolver,
    private val urlDetector: DevServerUrlDetector,
    private val startupTimeout: Duration,
    private val pollInterval: Duration,
    coroutineFeature: CoroutineFeature
) : DevServerController, CoroutineFeature by coroutineFeature {
    private val mutableState = MutableStateFlow<DevServerState>(DevServerState.Stopped)
    private val mutex = Mutex()

    /**
     * Incremented on every launch and on every stop. A Gradle run reports its exit asynchronously;
     * exits of runs that are no longer current must not overwrite the state of a newer run.
     */
    private val launchGeneration = AtomicInteger()

    override val state: StateFlow<DevServerState> = mutableState.asStateFlow()

    private fun isCurrent(generation: Int): Boolean = launchGeneration.get() == generation

    private fun markRunning(host: PreviewHost, baseUrl: String) {
        originResolver.remember(host, baseUrl)
        mutableState.value = DevServerState.Running(host, baseUrl)
    }

    /**
     * A successful exit does not mean the server is gone: Kobweb, for example, leaves its server
     * running after `kobwebStart` returns. The port decides.
     */
    private fun onRunExited(generation: Int, host: PreviewHost, qualifiedTaskName: String, isSuccess: Boolean) {
        if (!isCurrent(generation)) return
        if (!isSuccess) {
            mutableState.value = DevServerState.Failed(
                "Gradle task '$qualifiedTaskName' failed. See the Run tool window for details."
            )
            return
        }
        launch {
            val aliveBaseUrl = originResolver.findAlive(host)
            if (!isCurrent(generation)) return@launch
            if (aliveBaseUrl != null) markRunning(host, aliveBaseUrl) else mutableState.value = DevServerState.Stopped
        }
    }

    /**
     * Polls until the server answers at the origin announced by the run or, before the announcement,
     * at the expected one. Gives up on timeout, when the run fails and when the controller is stopped.
     */
    private suspend fun awaitServer(
        generation: Int,
        host: PreviewHost,
        announcedBaseUrl: CompletableDeferred<String>
    ): String? {
        val expectedBaseUrl = originResolver.expected(host)
        return withTimeoutOrNull(startupTimeout) {
            var aliveBaseUrl: String? = null
            while (aliveBaseUrl == null && isCurrent(generation) && mutableState.value is DevServerState.Starting) {
                val candidate = if (announcedBaseUrl.isCompleted) announcedBaseUrl.await() else expectedBaseUrl
                val isAlive = candidate != null && healthCheck.isAlive(candidate)
                if (isAlive) aliveBaseUrl = candidate else delay(pollInterval)
            }
            aliveBaseUrl
        }
    }

    private suspend fun launchAndAwait(host: PreviewHost) {
        val generation = launchGeneration.incrementAndGet()
        val announcedBaseUrl = CompletableDeferred<String>()
        val config = DevServerLaunchConfig.forHost(
            host = host,
            taskName = host.kind.startTask,
            arguments = host.kind.arguments
        )
        gradleTaskRunner.start(
            config = config,
            executionName = DevServerRunNames.devServer(host),
            listener = DevServerRunListener(
                urlDetector = urlDetector,
                onBaseUrlAnnounced = { baseUrl ->
                    // Remembered at once: a launch abandoned by a switch to another module must still
                    // be found alive, not started a second time, when the user comes back
                    originResolver.remember(host, baseUrl)
                    announcedBaseUrl.complete(baseUrl)
                },
                onExited = { isSuccess -> onRunExited(generation, host, config.qualifiedTaskName, isSuccess) }
            )
        )
        val aliveBaseUrl = awaitServer(generation, host, announcedBaseUrl)
        when {
            aliveBaseUrl != null -> markRunning(host, aliveBaseUrl)
            isCurrent(generation) && mutableState.value is DevServerState.Starting -> {
                mutableState.value = DevServerState.Failed(
                    "Dev server of ${host.displayName} did not answer within $startupTimeout. " +
                        "See the Run tool window for details."
                )
            }
        }
    }

    /**
     * Adopts a server that already answers or, when allowed, moves to [DevServerState.Starting].
     * Returns true when the caller must launch.
     *
     * A previous failure blocks automatic launches: a broken preview module would otherwise be
     * relaunched on every editor event, flooding the IDE with errors. Only explicit requests retry.
     * Adoption is never blocked, so a file whose server is up is shown even after another module failed.
     */
    private suspend fun adoptOrPrepareLaunch(host: PreviewHost, retryAfterFailure: Boolean): Boolean {
        val aliveBaseUrl = originResolver.findAlive(host)
        val isLaunchBlocked = mutableState.value is DevServerState.Failed && !retryAfterFailure
        when {
            aliveBaseUrl != null -> markRunning(host, aliveBaseUrl)
            isLaunchBlocked -> Unit
            else -> mutableState.value = DevServerState.Starting(host)
        }
        return aliveBaseUrl == null && !isLaunchBlocked
    }

    /** Decides under the lock whether a launch is needed. Returns true when the caller must launch. */
    private suspend fun prepareLaunch(host: PreviewHost, retryAfterFailure: Boolean): Boolean = mutex.withLock {
        val current = mutableState.value
        val isServingHost = current is DevServerState.Running && current.host == host
        when {
            current is DevServerState.Starting && current.host == host -> false
            isServingHost && healthCheck.isAlive(current.baseUrl) -> false
            else -> adoptOrPrepareLaunch(host, retryAfterFailure)
        }
    }

    /** Servers that outlive the Gradle run, such as Kobweb's, are stopped by their own task. */
    private suspend fun runStopTask(host: PreviewHost) {
        val stopTask = host.kind.stopTask ?: return
        gradleTaskRunner.start(
            config = DevServerLaunchConfig.forHost(host, taskName = stopTask, arguments = ""),
            executionName = DevServerRunNames.STOP_TASK,
            listener = SilentProcessListener
        )
    }

    override suspend fun ensureRunning(host: PreviewHost, retryAfterFailure: Boolean) {
        if (prepareLaunch(host, retryAfterFailure)) launchAndAwait(host)
    }

    override suspend fun stop() = mutex.withLock {
        launchGeneration.incrementAndGet()
        val host = mutableState.value.hostOrNull()
        if (host != null) {
            gradleTaskRunner.stop(DevServerRunNames.devServer(host))
            runStopTask(host)
            originResolver.forget(host)
        }
        mutableState.value = DevServerState.Stopped
    }

    override suspend fun restart(host: PreviewHost) {
        stop()
        ensureRunning(host, retryAfterFailure = true)
    }
}
