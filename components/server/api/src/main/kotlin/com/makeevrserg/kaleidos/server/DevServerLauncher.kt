package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.PreviewHost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * One dev server as a cold flow: collecting [launch] adopts the server of the module when it already
 * answers or starts the Gradle task of the module, and cancelling the collector stops what was started.
 * The run belongs to the collecting coroutine, so a closed project or a switch to another module can
 * never leave it behind.
 *
 * Only runs started here are stopped. A server the plugin did not start, from a terminal or an earlier
 * IDE session, is adopted as it is and left untouched, until the user asks for a restart: that is a
 * request to replace exactly such a server, which is the way out of one built without the page.
 */
class DevServerLauncher(
    private val gradleTaskRunner: GradleTaskRunner,
    private val detachedServerStopper: DetachedServerStopper,
    private val healthCheck: DevServerHealthCheck,
    private val originResolver: DevServerOriginResolver,
    private val portListenerLookup: PortListenerLookup,
    private val urlDetector: DevServerUrlDetector,
    private val startupTimeout: Duration,
    private val pollInterval: Duration
) {
    /**
     * Collecting the run is what hands the task to the platform, so the observer is launched as a
     * child of the flow and dies with it. It turns the event stream into the two facts the launch
     * waits for: the announced origin and the exit.
     */
    private fun CoroutineScope.observeRun(
        host: PreviewHost,
        config: DevServerLaunchConfig,
        signals: DevServerRunSignals
    ): Job = launch {
        val parser = AnnouncedBaseUrlParser(urlDetector)
        gradleTaskRunner.run(config, DevServerRunNames.devServer(host)).collect { event ->
            when (event) {
                GradleRunEvent.Started -> Unit
                is GradleRunEvent.Output -> parser.feed(event.text)?.let { baseUrl ->
                    signals.announcedBaseUrl.complete(baseUrl)
                }
                is GradleRunEvent.Exited -> signals.exit.complete(event.isSuccess)
            }
        }
    }

    /**
     * The detached server goes first: stopping it needs nothing from the IDE, while the run may already
     * have been destroyed by the platform when the project is closing.
     */
    private suspend fun stopRun(host: PreviewHost) {
        if (host.kind.isServerDetached) detachedServerStopper.stop(host)
        gradleTaskRunner.stop(DevServerRunNames.devServer(host))
    }

    private fun listenerDescription(listener: PortListener): String {
        val commandLine = listener.commandLine ?: return "It is process ${listener.pid}."
        return "It is process ${listener.pid}: $commandLine"
    }

    /**
     * Names the program in the way of the operating system, so the user can tell their own server from
     * something they forgot about without going looking for it. A machine that does not say who holds
     * the port leaves the address to speak for itself.
     */
    private suspend fun foreignOriginReason(host: PreviewHost, baseUrl: String): String {
        val address = "Another program is already listening at $baseUrl, where the dev server of " +
            "${host.displayName} would answer."
        val listener = LocalhostOrigin.portOf(baseUrl)?.let { port -> portListenerLookup.find(port) }
            ?: return "$address Stop it, or give the module a port of its own."
        return listOf(
            address,
            listenerDescription(listener),
            "Stop it with ${listener.stopCommand}, or give the module a port of its own."
        ).joinToString(separator = PARAGRAPH_SEPARATOR)
    }

    /**
     * Clears the address of the module for the run about to bind it, and answers whether it is free.
     *
     * A restart is a request to replace whatever holds that address, so the detached server the module
     * recorded for itself is stopped; nothing else is, which leaves a program of someone else there to
     * be reported instead of killed. An automatic launch stops nothing at all: it follows editor
     * events, and a server somebody may be using is not the plugin's to end without being asked.
     */
    private suspend fun ProducerScope<DevServerState>.freeOrigin(
        host: PreviewHost,
        options: DevServerLaunchOptions,
        isRestart: Boolean
    ): Boolean {
        if (isRestart && host.kind.isServerDetached) detachedServerStopper.stop(host)
        val foreignBaseUrl = originResolver.findForeign(host, options) ?: return true
        send(DevServerState.Failed(foreignOriginReason(host, foreignBaseUrl)))
        return false
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
     * An exit of the run does not mean the server is gone: Kobweb's server is a separate process that
     * stays up after `kobwebStart` returns, and also after the run is killed, by the user in the Run
     * tool window or by the platform when the project closes. The port decides: a server that still
     * answers stays owned until cancellation stops it. Returns only once nothing is left to stop.
     */
    private suspend fun ProducerScope<DevServerState>.serveAfterExit(
        host: PreviewHost,
        run: DevServerRunSignals,
        expectedBaseUrl: String?,
        qualifiedTaskName: String
    ) {
        val isSuccess = run.exit.await()
        val survivingBaseUrl = candidateBaseUrl(run, expectedBaseUrl)
            ?.takeIf { candidate -> healthCheck.isAlive(candidate) }
        if (survivingBaseUrl != null) {
            send(DevServerState.Running(host, survivingBaseUrl))
            awaitCancellation()
        }
        if (!isSuccess) {
            send(DevServerState.Failed("Gradle task '$qualifiedTaskName' failed. See the Run tool window for details."))
            return
        }
        send(DevServerState.Stopped)
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
    private suspend fun ProducerScope<DevServerState>.launchAndServe(
        host: PreviewHost,
        options: DevServerLaunchOptions,
        isRestart: Boolean
    ) {
        send(DevServerState.Starting(host))
        if (!freeOrigin(host, options, isRestart)) return
        val config = DevServerLaunchConfig.forHost(host, options)
        val expectedBaseUrl = originResolver.expected(host, options)
        val run = DevServerRunSignals(
            announcedBaseUrl = CompletableDeferred(),
            exit = CompletableDeferred()
        )
        observeRun(host, config, run)
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
     *
     * @param isRestart true when the user asked for the server of the module to be replaced: the one
     * that answers now is what is being replaced, so it is stopped and launched again, never adopted
     */
    fun launch(
        host: PreviewHost,
        options: DevServerLaunchOptions,
        isRestart: Boolean
    ): Flow<DevServerState> = channelFlow {
        val adoptedBaseUrl = if (isRestart) null else originResolver.findAlive(host, options)
        if (adoptedBaseUrl != null) {
            send(DevServerState.Running(host, adoptedBaseUrl))
            awaitCancellation()
        }
        launchAndServe(host, options, isRestart)
    }

    private companion object {
        const val PARAGRAPH_SEPARATOR = "\n\n"
    }
}
