package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.server.KobwebServerFixtures.writeConf
import com.makeevrserg.compose.html.preview.server.KobwebServerFixtures.writeServerState
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.options
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DevServerLauncherTest {
    private val kobwebDirectory: Path = Files.createTempDirectory("kobweb-module")
        .also { directory -> writeConf(directory, port = 8086) }

    private val kobwebHost = host(":instances:web-preview-kobweb", DevServerKind.KOBWEB, kobwebDirectory)

    private val webpackHost = host(":instances:web-preview", DevServerKind.WEBPACK)

    private val healthCheck = FakeDevServerHealthCheck()

    private val taskRunner = FakeGradleTaskRunner()

    private val detachedServerStopper = FakeDetachedServerStopper(healthCheck)

    private val launcher = DevServerLauncher(
        gradleTaskRunner = taskRunner,
        detachedServerStopper = detachedServerStopper,
        healthCheck = healthCheck,
        originResolver = DevServerOriginResolver(
            kobwebConfReader = KobwebConfReader(ioContext = EmptyCoroutineContext),
            kobwebServerStateReader = KobwebServerStateReader(ioContext = EmptyCoroutineContext),
            healthCheck = healthCheck
        ),
        urlDetector = DevServerUrlDetector(),
        startupTimeout = STARTUP_TIMEOUT,
        pollInterval = POLL_INTERVAL
    )

    private val states = mutableListOf<DevServerState>()

    /** Collects into [states] in the background; `runCurrent` and `advanceTimeBy` drive the launch. */
    private fun TestScope.collectStates(host: PreviewHost, isRestart: Boolean = false): Job {
        return backgroundScope.launch { launcher.launch(host, options(), isRestart).toList(states) }
    }

    /** Simulates the dev server printing its origin and answering at it. */
    private fun StartedRun.serverComesUp(host: PreviewHost, baseUrl: String) {
        printOutput("Loopback: $baseUrl/\n")
        healthCheck.aliveUrls += baseUrl
        if (host.kind.isServerDetached) detachedServerStopper.runningServers[host] = baseUrl
    }

    /** The Kobweb server of the module, up and recorded the way Kobweb records it. */
    private fun kobwebServerRuns() {
        writeServerState(kobwebDirectory, port = 8086, pid = ProcessHandle.current().pid())
        healthCheck.aliveUrls += "http://localhost:8086"
        detachedServerStopper.runningServers[kobwebHost] = "http://localhost:8086"
    }

    /** Launches the webpack server and brings it up; returns the collector that owns the run. */
    private fun TestScope.launchWebpackServer(): Job {
        val collector = collectStates(webpackHost)
        runCurrent()
        taskRunner.startedRuns.single().serverComesUp(webpackHost, "http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
        return collector
    }

    @AfterTest
    fun cleanUp() {
        kobwebDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_own_server_answers_at_conf_origin_WHEN_collected_THEN_adopted_without_gradle() = runTest {
        kobwebServerRuns()

        collectStates(kobwebHost)
        runCurrent()

        assertEquals(listOf<DevServerState>(DevServerState.Running(kobwebHost, "http://localhost:8086")), states)
        assertTrue(taskRunner.startedRuns.isEmpty())
    }

    @Test
    fun GIVEN_adopted_server_WHEN_collector_cancelled_THEN_nothing_is_stopped() = runTest {
        kobwebServerRuns()
        val collector = collectStates(kobwebHost)
        runCurrent()

        collector.cancel()
        runCurrent()

        assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
        assertTrue(taskRunner.startedRuns.isEmpty())
        assertTrue(detachedServerStopper.stoppedHosts.isEmpty())
    }

    /**
     * The server of another Kobweb module of the build, or of another project: every Kobweb module
     * declares the same default port, and a page from that server knows nothing of these previews.
     */
    @Test
    fun GIVEN_foreign_program_at_conf_origin_WHEN_collected_THEN_failed_and_nothing_launched() = runTest {
        healthCheck.aliveUrls += "http://localhost:8086"

        collectStates(kobwebHost)
        runCurrent()

        val state = assertIs<DevServerState.Failed>(states.last())
        assertTrue(state.reason.contains("http://localhost:8086"), state.reason)
        assertTrue(state.reason.contains(kobwebHost.displayName), state.reason)
        assertTrue(taskRunner.startedRuns.isEmpty())
    }

    @Test
    fun GIVEN_own_server_answers_WHEN_restart_THEN_it_is_stopped_and_launched_again() = runTest {
        kobwebServerRuns()

        collectStates(kobwebHost, isRestart = true)
        runCurrent()

        assertEquals(listOf<DevServerState>(DevServerState.Starting(kobwebHost)), states)
        assertEquals(listOf(kobwebHost), detachedServerStopper.stoppedHosts)
        val run = taskRunner.startedRuns.single()
        assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
    }

    @Test
    fun GIVEN_no_server_WHEN_collected_THEN_starting_and_start_task_of_the_kind_launched_in_the_module() = runTest {
        collectStates(webpackHost)
        runCurrent()

        assertEquals(listOf<DevServerState>(DevServerState.Starting(webpackHost)), states)
        val run = taskRunner.startedRuns.single()
        assertEquals(":instances:web-preview:jsBrowserDevelopmentRun", run.config.qualifiedTaskName)
        assertEquals("--continuous", run.config.arguments)
        assertEquals("/project", run.config.rootProjectPath)
        assertEquals(DevServerRunNames.devServer(webpackHost), run.executionName)
    }

    @Test
    fun GIVEN_run_announces_origin_and_server_answers_WHEN_polled_THEN_running_at_announced_origin() = runTest {
        launchWebpackServer()

        assertEquals(DevServerState.Running(webpackHost, "http://localhost:8085"), states.last())
    }

    @Test
    fun GIVEN_run_fails_before_serving_WHEN_exit_reported_THEN_failed_with_task_name_and_nothing_to_stop() = runTest {
        val collector = collectStates(webpackHost)
        runCurrent()

        taskRunner.startedRuns.single().exit(isSuccess = false)
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        val state = assertIs<DevServerState.Failed>(states.last())
        assertTrue(state.reason.contains(":instances:web-preview:jsBrowserDevelopmentRun"), state.reason)
        assertTrue(collector.isCompleted)
        assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
    }

    @Test
    fun GIVEN_server_never_answers_WHEN_startup_timeout_passes_THEN_failed_but_run_still_owned() = runTest {
        val collector = collectStates(webpackHost)
        runCurrent()

        advanceTimeBy(STARTUP_TIMEOUT + POLL_INTERVAL)
        runCurrent()

        val state = assertIs<DevServerState.Failed>(states.last())
        assertTrue(state.reason.contains("did not answer"), state.reason)
        assertFalse(collector.isCompleted)
        collector.cancel()
        runCurrent()
        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
    }

    @Test
    fun GIVEN_failed_by_timeout_WHEN_run_exits_successfully_and_server_answers_THEN_running() = runTest {
        collectStates(kobwebHost)
        runCurrent()
        advanceTimeBy(STARTUP_TIMEOUT + POLL_INTERVAL)
        runCurrent()
        val run = taskRunner.startedRuns.single()

        run.serverComesUp(kobwebHost, "http://localhost:8086")
        run.exit(isSuccess = true)
        runCurrent()

        assertEquals(DevServerState.Running(kobwebHost, "http://localhost:8086"), states.last())
    }

    @Test
    fun GIVEN_kobweb_run_finishes_but_server_keeps_answering_WHEN_exit_reported_THEN_still_running() = runTest {
        val collector = collectStates(kobwebHost)
        runCurrent()
        val run = taskRunner.startedRuns.single()
        run.serverComesUp(kobwebHost, "http://localhost:8086")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        run.exit(isSuccess = true)
        runCurrent()

        assertEquals(DevServerState.Running(kobwebHost, "http://localhost:8086"), states.last())
        assertFalse(collector.isCompleted)
    }

    @Test
    fun GIVEN_kobweb_run_killed_but_server_answers_WHEN_collector_cancelled_THEN_server_still_stopped() = runTest {
        val collector = collectStates(kobwebHost)
        runCurrent()
        val run = taskRunner.startedRuns.single()
        run.serverComesUp(kobwebHost, "http://localhost:8086")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        run.exit(isSuccess = false)
        runCurrent()
        val stateAfterKill = states.last()
        collector.cancel()
        runCurrent()

        assertEquals(DevServerState.Running(kobwebHost, "http://localhost:8086"), stateAfterKill)
        assertEquals(listOf(kobwebHost), detachedServerStopper.stoppedHosts)
    }

    @Test
    fun GIVEN_run_finishes_and_server_is_gone_WHEN_exit_reported_THEN_stopped_and_nothing_to_stop() = runTest {
        val collector = collectStates(webpackHost)
        runCurrent()
        val run = taskRunner.startedRuns.single()
        run.serverComesUp(webpackHost, "http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
        healthCheck.aliveUrls.clear()

        run.exit(isSuccess = true)
        runCurrent()

        assertEquals(DevServerState.Stopped, states.last())
        assertTrue(collector.isCompleted)
        assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
        assertTrue(detachedServerStopper.stoppedHosts.isEmpty())
    }

    @Test
    fun GIVEN_running_kobweb_server_WHEN_collector_cancelled_THEN_run_and_detached_server_stopped() = runTest {
        val collector = collectStates(kobwebHost)
        runCurrent()
        taskRunner.startedRuns.single().serverComesUp(kobwebHost, "http://localhost:8086")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        collector.cancel()
        runCurrent()

        assertEquals(listOf(DevServerRunNames.devServer(kobwebHost)), taskRunner.stoppedExecutionNames)
        assertEquals(listOf(kobwebHost), detachedServerStopper.stoppedHosts)
    }

    @Test
    fun GIVEN_running_webpack_server_WHEN_collector_cancelled_THEN_only_the_run_is_terminated() = runTest {
        val collector = launchWebpackServer()

        collector.cancel()
        runCurrent()

        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
        assertTrue(detachedServerStopper.stoppedHosts.isEmpty())
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_collector_cancelled_THEN_run_terminated() = runTest {
        val collector = collectStates(webpackHost)
        runCurrent()

        collector.cancel()
        runCurrent()

        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
    }

    private companion object {
        val STARTUP_TIMEOUT = 5.minutes
        val POLL_INTERVAL = 1.seconds
    }
}
