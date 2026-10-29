@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.server.usecase

import com.makeevrserg.kaleidos.host.model.DevServerKind
import com.makeevrserg.kaleidos.host.model.PreviewHost
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
import com.makeevrserg.kaleidos.server.model.DevServerLogEvent
import com.makeevrserg.kaleidos.server.model.DevServerState
import com.makeevrserg.kaleidos.server.model.PortListener
import com.makeevrserg.kaleidos.server.model.PreviewHostFixtures.host
import com.makeevrserg.kaleidos.server.model.PreviewHostFixtures.options
import com.makeevrserg.kaleidos.server.model.StartedRun
import com.makeevrserg.kaleidos.server.service.DefaultDevServerOriginResolver
import com.makeevrserg.kaleidos.server.util.devServerRunName
import com.makeevrserg.kaleidos.server.util.exit
import com.makeevrserg.kaleidos.server.util.printOutput
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
class LaunchDevServerUseCaseTest {
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

    private val log = DefaultDevServerLog(capacity = LOG_CAPACITY)

    private val launcher = LaunchDevServerUseCase(
        gradleTaskRunner = taskRunner,
        detachedServerStopper = detachedServerStopper,
        healthCheck = healthCheck,
        originResolver = DefaultDevServerOriginResolver(
            kobwebConfReader = DefaultKobwebConfReader(ioContext = EmptyCoroutineContext),
            kobwebServerStateReader = DefaultKobwebServerStateReader(ioContext = EmptyCoroutineContext),
            healthCheck = healthCheck
        ),
        portListenerLookup = portListenerLookup,
        announcedBaseUrlParser = { DefaultAnnouncedBaseUrlParser(DevServerUrlDetector()) },
        log = log,
        startupTimeout = STARTUP_TIMEOUT,
        pollInterval = POLL_INTERVAL
    )

    private val states = mutableListOf<DevServerState>()

    /** Collects into [states] in the background; `runCurrent` and `advanceTimeBy` drive the launch. */
    private fun TestScope.collectStates(host: PreviewHost, isRestart: Boolean = false): Job {
        return backgroundScope.launch {
            launcher.invoke(
                host,
                options(),
                isRestart
            ).toList(states)
        }
    }

    /** Simulates the dev server printing its origin and answering at it. */
    private fun StartedRun.serverComesUp(host: PreviewHost, baseUrl: String) {
        printOutput("Loopback: $baseUrl/\n")
        healthCheck.aliveUrls += baseUrl
        if (host.kind.isServerDetached) detachedServerStopper.runningServers[host] = baseUrl
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
    fun GIVEN_own_server_answers_at_conf_origin_WHEN_collected_THEN_adopted_without_gradle() {
        return runTest {
            kobwebServerRuns()

            collectStates(kobwebHost)
            runCurrent()

            assertEquals(listOf<DevServerState>(DevServerState.Running(kobwebHost, "http://localhost:8086")), states)
            assertTrue(taskRunner.startedRuns.isEmpty())
        }
    }

    @Test
    fun GIVEN_own_server_answers_WHEN_collected_THEN_the_log_names_the_adopted_server() {
        return runTest {
            kobwebServerRuns()

            collectStates(kobwebHost)
            runCurrent()

            assertEquals(
                listOf<DevServerLogEvent>(DevServerLogEvent.Adopted("http://localhost:8086")),
                log.events.replayCache
            )
        }
    }

    @Test
    fun GIVEN_no_server_WHEN_the_run_prints_THEN_the_log_holds_the_run_and_its_output() {
        return runTest {
            launchWebpackServer()

            assertEquals(
                listOf(
                    DevServerLogEvent.RunStarted(taskRunner.startedRuns.single().config),
                    DevServerLogEvent.Output("Loopback: http://localhost:8085/\n")
                ),
                log.events.replayCache
            )
        }
    }

    @Test
    fun GIVEN_restart_WHEN_the_new_run_starts_THEN_the_log_marks_where_it_begins() {
        return runTest {
            kobwebServerRuns()

            collectStates(kobwebHost, isRestart = true)
            runCurrent()

            assertEquals(
                listOf<DevServerLogEvent>(DevServerLogEvent.RunStarted(taskRunner.startedRuns.single().config)),
                log.events.replayCache
            )
        }
    }

    @Test
    fun GIVEN_adopted_server_WHEN_collector_cancelled_THEN_nothing_is_stopped() {
        return runTest {
            kobwebServerRuns()
            val collector = collectStates(kobwebHost)
            runCurrent()

            collector.cancel()
            runCurrent()

            assertTrue(taskRunner.stoppedExecutionNames.isEmpty())
            assertTrue(taskRunner.startedRuns.isEmpty())
            assertTrue(detachedServerStopper.stoppedHosts.isEmpty())
        }
    }

    /**
     * The server of another Kobweb module of the build, or of another project: every Kobweb module
     * declares the same default port, and a page from that server knows nothing of these previews.
     */
    @Test
    fun GIVEN_foreign_program_at_conf_origin_WHEN_collected_THEN_failed_and_nothing_launched() {
        return runTest {
            healthCheck.aliveUrls += "http://localhost:8086"

            collectStates(kobwebHost)
            runCurrent()

            val state = assertIs<DevServerState.Failed>(states.last())
            assertTrue(state.reason.contains("http://localhost:8086"), state.reason)
            assertTrue(state.reason.contains(kobwebHost.displayName), state.reason)
            assertTrue(taskRunner.startedRuns.isEmpty())
        }
    }

    /** Whose program it is decides whether the user stops it, so the message has to say. */
    @Test
    fun GIVEN_foreign_program_this_machine_names_WHEN_collected_THEN_failure_names_it_and_how_to_stop_it() {
        return runTest {
            healthCheck.aliveUrls += "http://localhost:8086"
            portListenerLookup.listenersByPort[8086] = PortListener(
                pid = 4242,
                commandLine = "java -jar /tmp/copy/instances/web-app/.kobweb/server/server.jar",
                stopCommand = "kill 4242"
            )

            collectStates(kobwebHost)
            runCurrent()

            val state = assertIs<DevServerState.Failed>(states.last())
            assertTrue(state.reason.contains("4242"), state.reason)
            assertTrue(state.reason.contains(".kobweb/server/server.jar"), state.reason)
            assertTrue(state.reason.contains("kill 4242"), state.reason)
        }
    }

    @Test
    fun GIVEN_foreign_program_without_a_command_line_WHEN_collected_THEN_failure_names_the_process_alone() {
        return runTest {
            healthCheck.aliveUrls += "http://localhost:8086"
            portListenerLookup.listenersByPort[8086] = PortListener(
                pid = 4242,
                commandLine = null,
                stopCommand = "kill 4242"
            )

            collectStates(kobwebHost)
            runCurrent()

            val state = assertIs<DevServerState.Failed>(states.last())
            assertTrue(state.reason.contains("It is process 4242."), state.reason)
            assertTrue(state.reason.contains("kill 4242"), state.reason)
        }
    }

    @Test
    fun GIVEN_own_server_answers_WHEN_restart_THEN_it_is_stopped_and_launched_again() {
        return runTest {
            kobwebServerRuns()

            collectStates(kobwebHost, isRestart = true)
            runCurrent()

            assertEquals(listOf<DevServerState>(DevServerState.Starting(kobwebHost)), states)
            assertEquals(listOf(kobwebHost), detachedServerStopper.stoppedHosts)
            val run = taskRunner.startedRuns.single()
            assertEquals(":instances:web-preview-kobweb:kobwebStart", run.config.qualifiedTaskName)
        }
    }

    @Test
    fun GIVEN_no_server_WHEN_collected_THEN_starting_and_start_task_of_the_kind_launched_in_the_module() {
        return runTest {
            collectStates(webpackHost)
            runCurrent()

            assertEquals(listOf<DevServerState>(DevServerState.Starting(webpackHost)), states)
            val run = taskRunner.startedRuns.single()
            assertEquals(":instances:web-preview:jsBrowserDevelopmentRun", run.config.qualifiedTaskName)
            assertEquals("--continuous", run.config.arguments)
            assertEquals("/project", run.config.rootProjectPath)
            assertEquals(devServerRunName(webpackHost), run.executionName)
        }
    }

    @Test
    fun GIVEN_run_announces_origin_and_server_answers_WHEN_polled_THEN_running_at_announced_origin() {
        return runTest {
            launchWebpackServer()

            assertEquals(DevServerState.Running(webpackHost, "http://localhost:8085"), states.last())
        }
    }

    @Test
    fun GIVEN_run_fails_before_serving_WHEN_exit_reported_THEN_failed_with_task_name_and_nothing_to_stop() {
        return runTest {
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
    }

    @Test
    fun GIVEN_server_never_answers_WHEN_startup_timeout_passes_THEN_failed_but_run_still_owned() {
        return runTest {
            val collector = collectStates(webpackHost)
            runCurrent()

            advanceTimeBy(STARTUP_TIMEOUT + POLL_INTERVAL)
            runCurrent()

            val state = assertIs<DevServerState.Failed>(states.last())
            assertTrue(state.reason.contains("did not answer"), state.reason)
            assertFalse(collector.isCompleted)
            collector.cancel()
            runCurrent()
            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
        }
    }

    @Test
    fun GIVEN_failed_by_timeout_WHEN_run_exits_successfully_and_server_answers_THEN_running() {
        return runTest {
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
    }

    @Test
    fun GIVEN_kobweb_run_finishes_but_server_keeps_answering_WHEN_exit_reported_THEN_still_running() {
        return runTest {
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
    }

    @Test
    fun GIVEN_kobweb_run_killed_but_server_answers_WHEN_collector_cancelled_THEN_server_still_stopped() {
        return runTest {
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
    }

    @Test
    fun GIVEN_run_finishes_and_server_is_gone_WHEN_exit_reported_THEN_stopped_and_nothing_to_stop() {
        return runTest {
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
    }

    @Test
    fun GIVEN_running_kobweb_server_WHEN_collector_cancelled_THEN_run_and_detached_server_stopped() {
        return runTest {
            val collector = collectStates(kobwebHost)
            runCurrent()
            taskRunner.startedRuns.single().serverComesUp(kobwebHost, "http://localhost:8086")
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            collector.cancel()
            runCurrent()

            assertEquals(listOf(devServerRunName(kobwebHost)), taskRunner.stoppedExecutionNames)
            assertEquals(listOf(kobwebHost), detachedServerStopper.stoppedHosts)
        }
    }

    @Test
    fun GIVEN_running_webpack_server_WHEN_collector_cancelled_THEN_only_the_run_is_terminated() {
        return runTest {
            val collector = launchWebpackServer()

            collector.cancel()
            runCurrent()

            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
            assertTrue(detachedServerStopper.stoppedHosts.isEmpty())
        }
    }

    @Test
    fun GIVEN_launch_in_progress_WHEN_collector_cancelled_THEN_run_terminated() {
        return runTest {
            val collector = collectStates(webpackHost)
            runCurrent()

            collector.cancel()
            runCurrent()

            assertEquals(listOf(devServerRunName(webpackHost)), taskRunner.stoppedExecutionNames)
        }
    }

    private companion object {
        val STARTUP_TIMEOUT = 5.minutes
        val POLL_INTERVAL = 1.seconds
        const val LOG_CAPACITY = 100
    }
}
