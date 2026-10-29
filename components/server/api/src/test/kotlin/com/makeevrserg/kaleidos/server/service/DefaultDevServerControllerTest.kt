@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.server.service

import com.makeevrserg.kaleidos.core.coroutine.memory.TestCoroutineFeature
import com.makeevrserg.kaleidos.host.model.DevServerKind
import com.makeevrserg.kaleidos.host.model.PreviewHost
import com.makeevrserg.kaleidos.server.api.DevServerController
import com.makeevrserg.kaleidos.server.fake.FakeDetachedServerStopper
import com.makeevrserg.kaleidos.server.fake.FakeDevServerHealthCheck
import com.makeevrserg.kaleidos.server.fake.FakeGradleTaskRunner
import com.makeevrserg.kaleidos.server.fake.FakePortListenerLookup
import com.makeevrserg.kaleidos.server.file.DefaultKobwebConfReader
import com.makeevrserg.kaleidos.server.file.DefaultKobwebServerStateReader
import com.makeevrserg.kaleidos.server.file.writeConf
import com.makeevrserg.kaleidos.server.file.writeServerState
import com.makeevrserg.kaleidos.server.memory.DefaultAnnouncedBaseUrlParser
import com.makeevrserg.kaleidos.server.memory.DefaultDevServerLog
import com.makeevrserg.kaleidos.server.memory.DevServerUrlDetector
import com.makeevrserg.kaleidos.server.model.DevServerState
import com.makeevrserg.kaleidos.server.model.PreviewHostFixtures.host
import com.makeevrserg.kaleidos.server.model.PreviewHostFixtures.options
import com.makeevrserg.kaleidos.server.model.StartedRun
import com.makeevrserg.kaleidos.server.usecase.LaunchDevServerUseCase
import com.makeevrserg.kaleidos.server.util.devServerRunName
import com.makeevrserg.kaleidos.server.util.exit
import com.makeevrserg.kaleidos.server.util.printOutput
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDevServerControllerTest {
    private val kobwebDirectory: Path = Files.createTempDirectory("kobweb-module")
        .also { directory -> writeConf(directory, port = 8086) }

    private val kobwebHost = host(
        ":instances:web-preview-kobweb",
        DevServerKind.KOBWEB,
        kobwebDirectory
    )

    private val webpackHost = host(":instances:web-preview", DevServerKind.WEBPACK)

    private val healthCheck = FakeDevServerHealthCheck()

    private val taskRunner = FakeGradleTaskRunner()

    private val detachedServerStopper = FakeDetachedServerStopper(healthCheck)

    private val portListenerLookup = FakePortListenerLookup()

    private val originResolver = DefaultDevServerOriginResolver(
        kobwebConfReader = DefaultKobwebConfReader(ioContext = EmptyCoroutineContext),
        kobwebServerStateReader = DefaultKobwebServerStateReader(ioContext = EmptyCoroutineContext),
        healthCheck = healthCheck
    )

    private fun createController(scope: CoroutineScope): DefaultDevServerController {
        return DefaultDevServerController(
            launcher = LaunchDevServerUseCase(
                gradleTaskRunner = taskRunner,
                detachedServerStopper = detachedServerStopper,
                healthCheck = healthCheck,
                originResolver = originResolver,
                portListenerLookup = portListenerLookup,
                announcedBaseUrlParser = { DefaultAnnouncedBaseUrlParser(DevServerUrlDetector()) },
                log = DefaultDevServerLog(capacity = LOG_CAPACITY),
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
    private fun StartedRun.serverComesUp(host: PreviewHost, baseUrl: String) {
        printOutput("Loopback: $baseUrl/\n")
        healthCheck.aliveUrls += baseUrl
        if (host.kind.isServerDetached) detachedServerStopper.runningServers[host] = baseUrl
    }

    private suspend fun TestScope.requestWebpackLaunch(controller: DevServerController): StartedRun {
        controller.requestRunning(
            webpackHost,
            options(),
            retryAfterFailure = false
        )
        runCurrent()
        return taskRunner.startedRuns.last()
    }

    private suspend fun TestScope.startWebpackServer(controller: DevServerController): StartedRun {
        val run = requestWebpackLaunch(controller)
        run.serverComesUp(webpackHost, "http://localhost:8085")
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

    /** The Kobweb server of the module, up and recorded the way Kobweb records it. */
    private fun kobwebServerRuns() {
        writeServerState(
            kobwebDirectory,
            port = 8086,
            pid = ProcessHandle.current().pid()
        )
        healthCheck.aliveUrls += "http://localhost:8086"
        detachedServerStopper.runningServers[kobwebHost] = "http://localhost:8086"
    }

    private fun assertRunning(controller: DevServerController, host: PreviewHost, baseUrl: String) {
        assertEquals(DevServerState.Running(host, baseUrl), controller.state.value)
    }

    private suspend fun TestScope.adoptKobwebServer(controller: DevServerController) {
        kobwebServerRuns()
        controller.requestRunning(
            kobwebHost,
            options(),
            retryAfterFailure = false
        )
        runCurrent()
        assertRunning(
            controller,
            kobwebHost,
            "http://localhost:8086"
        )
    }

    @AfterTest
    fun cleanUp() {
        kobwebDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_no_server_WHEN_request_running_THEN_starting_and_run_launched() {
        return runTest {
            val controller = createController()

            requestWebpackLaunch(controller)

            assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
            assertEquals(1, taskRunner.startedRuns.size)
        }
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_request_running_again_for_same_host_THEN_no_second_launch() {
        return runTest {
            val controller = createController()
            requestWebpackLaunch(controller)

            controller.requestRunning(
                webpackHost,
                options(),
                retryAfterFailure = true
            )
            runCurrent()

            assertEquals(1, taskRunner.startedRuns.size)
            assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
        }
    }

    @Test
    fun GIVEN_running_server_answers_WHEN_request_running_for_same_host_THEN_nothing_launched() {
        return runTest {
            val controller = createController()
            adoptKobwebServer(controller)

            controller.requestRunning(
                kobwebHost,
                options(),
                retryAfterFailure = true
            )
            runCurrent()

            assertRunning(
                controller,
                kobwebHost,
                "http://localhost:8086"
            )
            assertTrue(taskRunner.startedRuns.isEmpty())
        }
    }

    @Test
    fun GIVEN_running_server_stopped_answering_WHEN_request_running_THEN_relaunched() {
        return runTest {
            val controller = createController()
            adoptKobwebServer(controller)
            healthCheck.aliveUrls.clear()

            controller.requestRunning(
                kobwebHost,
                options(),
                retryAfterFailure = false
            )
            runCurrent()

            assertEquals(DevServerState.Starting(kobwebHost), controller.state.value)
            val run = taskRunner.startedRuns.single()
            assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
        }
    }

    @Test
    fun GIVEN_server_of_one_module_running_WHEN_another_module_requested_THEN_previous_run_stopped_and_new_launched() {
        return runTest {
            val controller = createController()
            startWebpackServer(controller)

            controller.requestRunning(
                kobwebHost,
                options(),
                retryAfterFailure = false
            )
            runCurrent()

            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
            assertEquals(DevServerState.Starting(kobwebHost), controller.state.value)
            val run = taskRunner.startedRuns.last()
            assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
        }
    }

    @Test
    fun GIVEN_previous_launch_failed_WHEN_automatic_request_THEN_no_relaunch() {
        return runTest {
            val controller = createController()
            failWebpackLaunch(controller)

            controller.requestRunning(
                webpackHost,
                options(),
                retryAfterFailure = false
            )
            runCurrent()

            assertIs<DevServerState.Failed>(controller.state.value)
            assertEquals(1, taskRunner.startedRuns.size)
        }
    }

    @Test
    fun GIVEN_previous_launch_failed_WHEN_explicit_request_THEN_relaunched() {
        return runTest {
            val controller = createController()
            failWebpackLaunch(controller)

            controller.requestRunning(
                webpackHost,
                options(),
                retryAfterFailure = true
            )
            runCurrent()

            assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
            assertEquals(2, taskRunner.startedRuns.size)
        }
    }

    @Test
    fun GIVEN_launch_of_one_module_failed_WHEN_another_module_already_answers_THEN_adopted() {
        return runTest {
            val controller = createController()
            failWebpackLaunch(controller)

            adoptKobwebServer(controller)

            assertEquals(1, taskRunner.startedRuns.size)
        }
    }

    @Test
    fun GIVEN_launch_of_one_module_failed_WHEN_silent_module_requested_automatically_THEN_not_launched() {
        return runTest {
            val controller = createController()
            failWebpackLaunch(controller)

            controller.requestRunning(
                kobwebHost,
                options(),
                retryAfterFailure = false
            )
            runCurrent()

            assertIs<DevServerState.Failed>(controller.state.value)
            assertEquals(1, taskRunner.startedRuns.size)
        }
    }

    @Test
    fun GIVEN_running_server_WHEN_stop_THEN_stopped_and_run_terminated() {
        return runTest {
            val controller = createController()
            startWebpackServer(controller)

            controller.stop()
            runCurrent()

            assertEquals(DevServerState.Stopped, controller.state.value)
            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
        }
    }

    @Test
    fun GIVEN_stopped_server_WHEN_request_running_THEN_launched_again() {
        return runTest {
            val controller = createController()
            startWebpackServer(controller)
            controller.stop()
            runCurrent()
            healthCheck.aliveUrls.clear()

            controller.requestRunning(
                webpackHost,
                options(),
                retryAfterFailure = false
            )
            runCurrent()

            assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
            assertEquals(2, taskRunner.startedRuns.size)
        }
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_restart_THEN_old_run_stopped_new_launched_and_stale_exit_ignored() {
        return runTest {
            val controller = createController()
            val firstRun = requestWebpackLaunch(controller)

            controller.restart(webpackHost, options())
            runCurrent()
            firstRun.exit(isSuccess = false)
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            assertEquals(DevServerState.Starting(webpackHost), controller.state.value)
            assertEquals(2, taskRunner.startedRuns.size)
            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
        }
    }

    /**
     * The way out of a server that answers but was built without the previews of the page: adopting it
     * again is what the user is asking the plugin to stop doing.
     */
    @Test
    fun GIVEN_adopted_server_WHEN_restart_THEN_it_is_stopped_and_the_run_launched() {
        return runTest {
            val controller = createController()
            adoptKobwebServer(controller)

            controller.restart(kobwebHost, options())
            runCurrent()

            assertEquals(DevServerState.Starting(kobwebHost), controller.state.value)
            assertEquals(listOf(kobwebHost), detachedServerStopper.stoppedHosts)
            val run = taskRunner.startedRuns.single()
            assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
        }
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_owning_scope_cancelled_THEN_run_stopped() {
        return runTest {
            val scope = CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext[Job]))
            val controller = createController(scope)
            requestWebpackLaunch(controller)

            scope.cancel()
            runCurrent()

            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
        }
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_origin_awaited_with_first_THEN_server_keeps_running_afterwards() {
        return runTest {
            val controller = createController()
            val run = requestWebpackLaunch(controller)
            val awaitedRunning = async { controller.state.filterIsInstance<DevServerState.Running>().first() }

            run.serverComesUp(webpackHost, "http://localhost:8085")
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            assertEquals(DevServerState.Running(webpackHost, "http://localhost:8085"), awaitedRunning.await())
            assertRunning(
                controller,
                webpackHost,
                "http://localhost:8085"
            )
            assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
        }
    }

    private companion object {
        val STARTUP_TIMEOUT = 5.minutes
        val POLL_INTERVAL = 1.seconds
        const val LOG_CAPACITY = 100
    }
}
