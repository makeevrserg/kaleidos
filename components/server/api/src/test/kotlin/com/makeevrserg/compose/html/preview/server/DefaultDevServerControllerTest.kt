package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.core.TestCoroutineFeature
import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDevServerControllerTest {
    private val kobwebDirectory: Path = Files.createTempDirectory("kobweb-module").also { directory ->
        directory.resolve(".kobweb").createDirectories()
        directory.resolve(".kobweb/conf.yaml").writeText("server:\n  port: 8086\n")
    }

    private val kobwebHost = host(":instances:web-preview-kobweb", DevServerKind.KOBWEB, kobwebDirectory)

    private val webpackHost = host(":instances:web-preview", DevServerKind.WEBPACK)

    private val healthCheck = FakeDevServerHealthCheck()

    private val taskRunner = FakeGradleTaskRunner()

    private val detachedServerStopper = FakeDetachedServerStopper()

    private val originResolver = DevServerOriginResolver(
        kobwebConfReader = KobwebConfReader(ioContext = EmptyCoroutineContext),
        healthCheck = healthCheck
    )

    private fun createController(scope: CoroutineScope): DefaultDevServerController {
        return DefaultDevServerController(
            launcher = DevServerLauncher(
                gradleTaskRunner = taskRunner,
                detachedServerStopper = detachedServerStopper,
                healthCheck = healthCheck,
                originResolver = originResolver,
                urlDetector = DevServerUrlDetector(),
                startupTimeout = STARTUP_TIMEOUT,
                pollInterval = POLL_INTERVAL
            ),
            originResolver = originResolver,
            healthCheck = healthCheck,
            coroutineFeature = TestCoroutineFeature(scope)
        )
    }

    private fun TestScope.createController(): DefaultDevServerController = createController(backgroundScope)

    /** Simulates the dev server printing its origin and answering at it. */
    private fun StartedRun.serverComesUp(baseUrl: String) {
        printOutput("Loopback: $baseUrl/\n")
        healthCheck.aliveUrls += baseUrl
    }

    private suspend fun TestScope.requestWebpackLaunch(controller: DevServerController): StartedRun {
        controller.requestRunning(webpackHost, retryAfterFailure = false)
        runCurrent()
        return taskRunner.startedRuns.last()
    }

    private suspend fun TestScope.startWebpackServer(controller: DevServerController): StartedRun {
        val run = requestWebpackLaunch(controller)
        run.serverComesUp("http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
        return run
    }

    private suspend fun TestScope.failWebpackLaunch(controller: DevServerController) {
        requestWebpackLaunch(controller).exit(isSuccess = false)
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
        assertIs<DevServerState.Failed>(controller.state.value)
    }

    private suspend fun TestScope.adoptKobwebServer(controller: DevServerController) {
        healthCheck.aliveUrls += "http://localhost:8086"
        controller.requestRunning(kobwebHost, retryAfterFailure = false)
        runCurrent()
        assertRunning(controller, kobwebHost, "http://localhost:8086")
    }

    private fun assertRunning(controller: DevServerController, host: PreviewHost, baseUrl: String) {
        assertEquals(DevServerState.Running(host, baseUrl), controller.state.value)
    }

    @AfterTest
    fun cleanUp() {
        kobwebDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_no_server_WHEN_requestRunning_THEN_starting_and_run_launched() = runTest {
        val controller = createController()

        requestWebpackLaunch(controller)

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(1, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_requestRunning_again_for_same_host_THEN_no_second_launch() = runTest {
        val controller = createController()
        requestWebpackLaunch(controller)

        controller.requestRunning(webpackHost, retryAfterFailure = true)
        runCurrent()

        assertEquals(1, taskRunner.startedRuns.size)
        assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
    }

    @Test
    fun GIVEN_running_server_answers_WHEN_requestRunning_for_same_host_THEN_nothing_launched() = runTest {
        val controller = createController()
        adoptKobwebServer(controller)

        controller.requestRunning(kobwebHost, retryAfterFailure = true)
        runCurrent()

        assertRunning(controller, kobwebHost, "http://localhost:8086")
        assertTrue(taskRunner.startedRuns.isEmpty())
    }

    @Test
    fun GIVEN_running_server_stopped_answering_WHEN_requestRunning_THEN_relaunched() = runTest {
        val controller = createController()
        adoptKobwebServer(controller)
        healthCheck.aliveUrls.clear()

        controller.requestRunning(kobwebHost, retryAfterFailure = false)
        runCurrent()

        assertEquals(DevServerState.Starting(kobwebHost), controller.state.value)
        val run = taskRunner.startedRuns.single()
        assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
    }

    @Test
    fun GIVEN_server_of_one_module_running_WHEN_another_module_requested_THEN_previous_run_stopped_and_new_launched() =
        runTest {
            val controller = createController()
            startWebpackServer(controller)

            controller.requestRunning(kobwebHost, retryAfterFailure = false)
            runCurrent()

            assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
            assertEquals(DevServerState.Starting(kobwebHost), controller.state.value)
            val run = taskRunner.startedRuns.last()
            assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
        }

    @Test
    fun GIVEN_previous_launch_failed_WHEN_automatic_request_THEN_no_relaunch() = runTest {
        val controller = createController()
        failWebpackLaunch(controller)

        controller.requestRunning(webpackHost, retryAfterFailure = false)
        runCurrent()

        assertIs<DevServerState.Failed>(controller.state.value)
        assertEquals(1, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_previous_launch_failed_WHEN_explicit_request_THEN_relaunched() = runTest {
        val controller = createController()
        failWebpackLaunch(controller)

        controller.requestRunning(webpackHost, retryAfterFailure = true)
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(2, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_launch_of_one_module_failed_WHEN_another_module_already_answers_THEN_adopted() = runTest {
        val controller = createController()
        failWebpackLaunch(controller)

        adoptKobwebServer(controller)

        assertEquals(1, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_launch_of_one_module_failed_WHEN_silent_module_requested_automatically_THEN_not_launched() = runTest {
        val controller = createController()
        failWebpackLaunch(controller)

        controller.requestRunning(kobwebHost, retryAfterFailure = false)
        runCurrent()

        assertIs<DevServerState.Failed>(controller.state.value)
        assertEquals(1, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_running_server_WHEN_stop_THEN_stopped_and_run_terminated() = runTest {
        val controller = createController()
        startWebpackServer(controller)

        controller.stop()
        runCurrent()

        assertEquals(DevServerState.Stopped, controller.state.value)
        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
    }

    @Test
    fun GIVEN_stopped_server_WHEN_requestRunning_THEN_launched_again() = runTest {
        val controller = createController()
        startWebpackServer(controller)
        controller.stop()
        runCurrent()
        healthCheck.aliveUrls.clear()

        controller.requestRunning(webpackHost, retryAfterFailure = false)
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(2, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_restart_THEN_old_run_stopped_new_launched_and_stale_exit_ignored() = runTest {
        val controller = createController()
        val firstRun = requestWebpackLaunch(controller)

        controller.restart(webpackHost)
        runCurrent()
        firstRun.exit(isSuccess = false)
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(2, taskRunner.startedRuns.size)
        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_owning_scope_cancelled_THEN_run_stopped() = runTest {
        val scope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]))
        val controller = createController(scope)
        requestWebpackLaunch(controller)

        scope.cancel()
        runCurrent()

        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_origin_awaited_with_first_THEN_server_keeps_running_afterwards() = runTest {
        val controller = createController()
        val run = requestWebpackLaunch(controller)
        val awaitedRunning = async { controller.state.filterIsInstance<DevServerState.Running>().first() }

        run.serverComesUp("http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        assertEquals(DevServerState.Running(webpackHost, "http://localhost:8085"), awaitedRunning.await())
        assertRunning(controller, webpackHost, "http://localhost:8085")
        assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
    }

    private companion object {
        val STARTUP_TIMEOUT = 5.minutes
        val POLL_INTERVAL = 1.seconds
    }
}
