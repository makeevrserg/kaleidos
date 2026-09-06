package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.core.TestCoroutineFeature
import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

    private val originResolver = DevServerOriginResolver(
        kobwebConfReader = KobwebConfReader(ioContext = EmptyCoroutineContext),
        healthCheck = healthCheck
    )

    private fun TestScope.createController(): DefaultDevServerController {
        return DefaultDevServerController(
            healthCheck = healthCheck,
            gradleTaskRunner = taskRunner,
            originResolver = originResolver,
            urlDetector = DevServerUrlDetector(),
            startupTimeout = STARTUP_TIMEOUT,
            pollInterval = POLL_INTERVAL,
            coroutineFeature = TestCoroutineFeature(backgroundScope)
        )
    }

    /** Simulates the dev server printing its origin and answering at it. */
    private fun StartedRun.serverComesUp(baseUrl: String) {
        listener.onOutput("Loopback: $baseUrl/\n")
        healthCheck.aliveUrls += baseUrl
    }

    private fun assertRunning(controller: DevServerController, host: PreviewHost, baseUrl: String) {
        assertEquals(DevServerState.Running(host, baseUrl), controller.state.value)
    }

    @AfterTest
    fun cleanUp() {
        kobwebDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_server_already_answers_at_conf_origin_WHEN_ensureRunning_THEN_adopted_without_gradle() = runTest {
        val controller = createController()
        healthCheck.aliveUrls += "http://localhost:8086"

        controller.ensureRunning(kobwebHost, retryAfterFailure = false)

        assertRunning(controller, kobwebHost, "http://localhost:8086")
        assertTrue(taskRunner.startedRuns.isEmpty())
    }

    @Test
    fun GIVEN_no_server_WHEN_ensureRunning_THEN_start_task_of_the_kind_is_launched_in_the_module() = runTest {
        val controller = createController()

        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        val run = taskRunner.devServerRuns.single()
        assertEquals(":instances:web-preview:jsBrowserDevelopmentRun", run.config.qualifiedTaskName)
        assertEquals("--continuous", run.config.arguments)
        assertEquals("/project", run.config.rootProjectPath)
        assertEquals(DevServerRunNames.devServer(webpackHost), run.executionName)
        controller.stop()
    }

    @Test
    fun GIVEN_run_announces_origin_and_server_answers_WHEN_polled_THEN_running_at_announced_origin() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        taskRunner.devServerRuns.single().serverComesUp("http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        assertRunning(controller, webpackHost, "http://localhost:8085")
    }

    @Test
    fun GIVEN_run_fails_before_serving_WHEN_exit_reported_THEN_failed_with_task_name() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        taskRunner.devServerRuns.single().listener.onExited(isSuccess = false)
        runCurrent()

        val state = assertIs<DevServerState.Failed>(controller.state.value)
        assertTrue(state.reason.contains(":instances:web-preview:jsBrowserDevelopmentRun"), state.reason)
    }

    @Test
    fun GIVEN_server_never_answers_WHEN_startup_timeout_passes_THEN_failed() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        advanceTimeBy(STARTUP_TIMEOUT + POLL_INTERVAL)
        runCurrent()

        val state = assertIs<DevServerState.Failed>(controller.state.value)
        assertTrue(state.reason.contains("did not answer"), state.reason)
    }

    @Test
    fun GIVEN_previous_launch_failed_WHEN_automatic_request_THEN_no_relaunch() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        taskRunner.devServerRuns.single().listener.onExited(isSuccess = false)
        runCurrent()

        controller.ensureRunning(webpackHost, retryAfterFailure = false)

        assertIs<DevServerState.Failed>(controller.state.value)
        assertEquals(1, taskRunner.devServerRuns.size)
    }

    @Test
    fun GIVEN_previous_launch_failed_WHEN_explicit_request_THEN_relaunched() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        taskRunner.devServerRuns.single().listener.onExited(isSuccess = false)
        runCurrent()

        launch { controller.ensureRunning(webpackHost, retryAfterFailure = true) }
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(2, taskRunner.devServerRuns.size)
        controller.stop()
    }

    @Test
    fun GIVEN_launch_of_one_module_failed_WHEN_another_module_already_answers_THEN_adopted() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        taskRunner.devServerRuns.single().listener.onExited(isSuccess = false)
        runCurrent()
        healthCheck.aliveUrls += "http://localhost:8086"

        controller.ensureRunning(kobwebHost, retryAfterFailure = false)

        assertRunning(controller, kobwebHost, "http://localhost:8086")
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_ensureRunning_again_for_same_host_THEN_no_second_launch() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        launch { controller.ensureRunning(webpackHost, retryAfterFailure = true) }
        runCurrent()

        assertEquals(1, taskRunner.devServerRuns.size)
        controller.stop()
    }

    @Test
    fun GIVEN_running_server_stopped_answering_WHEN_ensureRunning_THEN_relaunched() = runTest {
        val controller = createController()
        healthCheck.aliveUrls += "http://localhost:8086"
        controller.ensureRunning(kobwebHost, retryAfterFailure = false)
        healthCheck.aliveUrls.clear()

        launch { controller.ensureRunning(kobwebHost, retryAfterFailure = false) }
        runCurrent()

        assertEquals(DevServerState.Starting(kobwebHost), controller.state.value)
        val run = taskRunner.devServerRuns.single()
        assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
        controller.stop()
    }

    @Test
    fun GIVEN_running_kobweb_server_WHEN_stop_THEN_run_terminated_stop_task_executed_and_origin_forgotten() = runTest {
        val controller = createController()
        healthCheck.aliveUrls += "http://localhost:8086"
        controller.ensureRunning(kobwebHost, retryAfterFailure = false)
        originResolver.remember(kobwebHost, "http://localhost:8086")

        controller.stop()

        assertEquals(DevServerState.Stopped, controller.state.value)
        assertEquals(listOf(DevServerRunNames.devServer(kobwebHost)), taskRunner.stoppedExecutionNames)
        val stopRun = taskRunner.startedRuns.single()
        assertEquals(DevServerRunNames.STOP_TASK, stopRun.executionName)
        assertEquals(":instances:web-preview-kobweb:kobwebStop", stopRun.config.qualifiedTaskName)
    }

    @Test
    fun GIVEN_running_webpack_server_WHEN_stop_THEN_only_the_run_is_terminated() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        taskRunner.devServerRuns.single().serverComesUp("http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        controller.stop()

        assertEquals(DevServerState.Stopped, controller.state.value)
        assertEquals(listOf(DevServerRunNames.devServer(webpackHost)), taskRunner.stoppedExecutionNames)
        assertEquals(1, taskRunner.startedRuns.size)
    }

    @Test
    fun GIVEN_stopped_webpack_server_WHEN_ensureRunning_THEN_launched_again_instead_of_adopting_old_origin() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        taskRunner.devServerRuns.single().serverComesUp("http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
        controller.stop()
        healthCheck.aliveUrls.clear()

        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(2, taskRunner.devServerRuns.size)
        controller.stop()
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_stop_THEN_stopped_and_pending_launch_gives_up() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()

        controller.stop()
        advanceTimeBy(STARTUP_TIMEOUT + POLL_INTERVAL)
        runCurrent()

        assertEquals(DevServerState.Stopped, controller.state.value)
    }

    @Test
    fun GIVEN_restart_WHEN_old_run_reports_failure_afterwards_THEN_stale_exit_is_ignored() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        val firstRun = taskRunner.devServerRuns.single()

        launch { controller.restart(webpackHost) }
        runCurrent()
        firstRun.listener.onExited(isSuccess = false)
        runCurrent()

        assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
        assertEquals(2, taskRunner.devServerRuns.size)
        controller.stop()
    }

    @Test
    fun GIVEN_kobweb_run_finishes_but_server_keeps_answering_WHEN_exit_reported_THEN_still_running() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(kobwebHost, retryAfterFailure = false) }
        runCurrent()
        val run = taskRunner.devServerRuns.single()
        run.serverComesUp("http://localhost:8086")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()

        run.listener.onExited(isSuccess = true)
        runCurrent()

        assertRunning(controller, kobwebHost, "http://localhost:8086")
    }

    @Test
    fun GIVEN_run_finishes_and_server_is_gone_WHEN_exit_reported_THEN_stopped() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        val run = taskRunner.devServerRuns.single()
        run.serverComesUp("http://localhost:8085")
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
        healthCheck.aliveUrls.clear()

        run.listener.onExited(isSuccess = true)
        runCurrent()

        assertEquals(DevServerState.Stopped, controller.state.value)
    }

    @Test
    fun GIVEN_abandoned_launch_announced_origin_WHEN_module_requested_again_THEN_adopted_without_relaunch() = runTest {
        val controller = createController()
        launch { controller.ensureRunning(webpackHost, retryAfterFailure = false) }
        runCurrent()
        val run = taskRunner.devServerRuns.single()
        run.listener.onOutput("Loopback: http://localhost:8085/\n")
        healthCheck.aliveUrls += "http://localhost:8086"
        controller.ensureRunning(kobwebHost, retryAfterFailure = false)
        healthCheck.aliveUrls += "http://localhost:8085"

        controller.ensureRunning(webpackHost, retryAfterFailure = false)

        assertRunning(controller, webpackHost, "http://localhost:8085")
        assertEquals(1, taskRunner.devServerRuns.size)
    }

    private companion object {
        val STARTUP_TIMEOUT = 5.minutes
        val POLL_INTERVAL = 1.seconds
    }
}
