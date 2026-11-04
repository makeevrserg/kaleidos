@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.server.api.service

import com.makeevrserg.kaleidos.host.api.model.DevServerKind
import com.makeevrserg.kaleidos.server.api.fake.FakeDevServerHealthCheck
import com.makeevrserg.kaleidos.server.api.file.DefaultKobwebConfReader
import com.makeevrserg.kaleidos.server.api.file.DefaultKobwebServerStateReader
import com.makeevrserg.kaleidos.server.api.file.writeConf
import com.makeevrserg.kaleidos.server.api.file.writeServerState
import com.makeevrserg.kaleidos.server.api.model.PreviewHostFixtures.host
import com.makeevrserg.kaleidos.server.api.model.PreviewHostFixtures.options
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultDevServerOriginResolverTest {
    private val kobwebDirectory: Path = Files.createTempDirectory("kobweb-module")
        .also { directory -> writeConf(directory, port = 8086) }

    private val kobwebHost = host(
        ":instances:web-preview-kobweb",
        DevServerKind.KOBWEB,
        kobwebDirectory
    )

    private val webpackHost = host(":instances:web-preview", DevServerKind.WEBPACK)

    private val healthCheck = FakeDevServerHealthCheck()

    private val resolver = DefaultDevServerOriginResolver(
        kobwebConfReader = DefaultKobwebConfReader(ioContext = EmptyCoroutineContext),
        kobwebServerStateReader = DefaultKobwebServerStateReader(ioContext = EmptyCoroutineContext),
        healthCheck = healthCheck
    )

    /** The server of the module, recorded the way Kobweb records it: this JVM stands in for it. */
    private fun ownServerRuns(port: Int) {
        writeServerState(
            kobwebDirectory,
            port = port,
            pid = ProcessHandle.current().pid()
        )
    }

    @AfterTest
    fun cleanUp() {
        kobwebDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_kobweb_module_never_run_WHEN_expected_THEN_origin_from_conf(): TestResult {
        return runTest {
            assertEquals("http://localhost:8086", resolver.expected(kobwebHost, options()))
        }
    }

    @Test
    fun GIVEN_webpack_module_WHEN_expected_THEN_the_port_the_plugin_chose(): TestResult {
        return runTest {
            assertEquals("http://localhost:8301", resolver.expected(webpackHost, options(devServerPort = 8301)))
        }
    }

    @Test
    fun GIVEN_webpack_module_without_a_port_WHEN_expected_THEN_unknown(): TestResult {
        return runTest {
            assertNull(resolver.expected(webpackHost, options()))
        }
    }

    @Test
    fun GIVEN_own_kobweb_server_answers_WHEN_find_alive_THEN_origin(): TestResult {
        return runTest {
            ownServerRuns(port = 8086)
            healthCheck.aliveUrls += "http://localhost:8086"

            assertEquals("http://localhost:8086", resolver.findAlive(kobwebHost, options()))
            assertNull(resolver.findForeign(kobwebHost, options()))
        }
    }

    /** Another Kobweb module of the build, or another project, declaring the same port answers as well. */
    @Test
    fun GIVEN_module_ran_no_server_but_the_port_answers_WHEN_find_alive_THEN_not_adopted_but_foreign(): TestResult {
        return runTest {
            healthCheck.aliveUrls += "http://localhost:8086"

            assertNull(resolver.findAlive(kobwebHost, options()))
            assertEquals("http://localhost:8086", resolver.findForeign(kobwebHost, options()))
        }
    }

    @Test
    fun GIVEN_own_server_on_another_port_WHEN_find_alive_THEN_not_adopted_but_foreign(): TestResult {
        return runTest {
            ownServerRuns(port = 9090)
            healthCheck.aliveUrls += "http://localhost:8086"

            assertNull(resolver.findAlive(kobwebHost, options()))
            assertEquals("http://localhost:8086", resolver.findForeign(kobwebHost, options()))
        }
    }

    @Test
    fun GIVEN_own_server_recorded_but_silent_WHEN_find_alive_THEN_null_and_nothing_foreign(): TestResult {
        return runTest {
            ownServerRuns(port = 8086)

            assertNull(resolver.findAlive(kobwebHost, options()))
            assertNull(resolver.findForeign(kobwebHost, options()))
        }
    }

    /** A webpack server dies with its run and records nothing; its port is the one the plugin picked free. */
    @Test
    fun GIVEN_webpack_server_answers_at_allocated_port_WHEN_find_alive_THEN_origin_and_nothing_foreign(): TestResult {
        return runTest {
            healthCheck.aliveUrls += "http://localhost:8301"

            assertEquals("http://localhost:8301", resolver.findAlive(webpackHost, options(devServerPort = 8301)))
            assertNull(resolver.findForeign(webpackHost, options(devServerPort = 8301)))
        }
    }

    @Test
    fun GIVEN_nothing_answers_WHEN_find_alive_THEN_null(): TestResult {
        return runTest {
            ownServerRuns(port = 8086)

            assertNull(resolver.findAlive(kobwebHost, options()))
            assertNull(resolver.findAlive(webpackHost, options(devServerPort = 8301)))
        }
    }

    @Test
    fun GIVEN_module_without_a_known_origin_WHEN_find_alive_or_find_foreign_THEN_null(): TestResult {
        return runTest {
            assertNull(resolver.findAlive(webpackHost, options()))
            assertNull(resolver.findForeign(webpackHost, options()))
        }
    }
}
