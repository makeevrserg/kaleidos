package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.DevServerKind
import com.makeevrserg.kaleidos.server.KobwebServerFixtures.writeConf
import com.makeevrserg.kaleidos.server.KobwebServerFixtures.writeServerState
import com.makeevrserg.kaleidos.server.PreviewHostFixtures.host
import com.makeevrserg.kaleidos.server.PreviewHostFixtures.options
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DevServerOriginResolverTest {
    private val kobwebDirectory: Path = Files.createTempDirectory("kobweb-module")
        .also { directory -> writeConf(directory, port = 8086) }

    private val kobwebHost = host(":instances:web-preview-kobweb", DevServerKind.KOBWEB, kobwebDirectory)

    private val webpackHost = host(":instances:web-preview", DevServerKind.WEBPACK)

    private val healthCheck = FakeDevServerHealthCheck()

    private val resolver = DevServerOriginResolver(
        kobwebConfReader = KobwebConfReader(ioContext = EmptyCoroutineContext),
        kobwebServerStateReader = KobwebServerStateReader(ioContext = EmptyCoroutineContext),
        healthCheck = healthCheck
    )

    /** The server of the module, recorded the way Kobweb records it: this JVM stands in for it. */
    private fun ownServerRuns(port: Int) {
        writeServerState(kobwebDirectory, port = port, pid = ProcessHandle.current().pid())
    }

    @AfterTest
    fun cleanUp() {
        kobwebDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_kobweb_module_never_run_WHEN_expected_THEN_origin_from_conf() = runTest {
        assertEquals("http://localhost:8086", resolver.expected(kobwebHost, options()))
    }

    @Test
    fun GIVEN_webpack_module_WHEN_expected_THEN_the_port_the_plugin_chose() = runTest {
        assertEquals("http://localhost:8301", resolver.expected(webpackHost, options(devServerPort = 8301)))
    }

    @Test
    fun GIVEN_webpack_module_without_a_port_WHEN_expected_THEN_unknown() = runTest {
        assertNull(resolver.expected(webpackHost, options()))
    }

    @Test
    fun GIVEN_own_kobweb_server_answers_WHEN_findAlive_THEN_origin() = runTest {
        ownServerRuns(port = 8086)
        healthCheck.aliveUrls += "http://localhost:8086"

        assertEquals("http://localhost:8086", resolver.findAlive(kobwebHost, options()))
        assertNull(resolver.findForeign(kobwebHost, options()))
    }

    /** Another Kobweb module of the build, or another project, declaring the same port answers as well. */
    @Test
    fun GIVEN_module_ran_no_server_but_the_port_answers_WHEN_findAlive_THEN_not_adopted_but_foreign() = runTest {
        healthCheck.aliveUrls += "http://localhost:8086"

        assertNull(resolver.findAlive(kobwebHost, options()))
        assertEquals("http://localhost:8086", resolver.findForeign(kobwebHost, options()))
    }

    @Test
    fun GIVEN_own_server_on_another_port_WHEN_findAlive_THEN_not_adopted_but_foreign() = runTest {
        ownServerRuns(port = 9090)
        healthCheck.aliveUrls += "http://localhost:8086"

        assertNull(resolver.findAlive(kobwebHost, options()))
        assertEquals("http://localhost:8086", resolver.findForeign(kobwebHost, options()))
    }

    @Test
    fun GIVEN_own_server_recorded_but_silent_WHEN_findAlive_THEN_null_and_nothing_foreign() = runTest {
        ownServerRuns(port = 8086)

        assertNull(resolver.findAlive(kobwebHost, options()))
        assertNull(resolver.findForeign(kobwebHost, options()))
    }

    /** A webpack server dies with its run and records nothing; its port is the one the plugin picked free. */
    @Test
    fun GIVEN_webpack_server_answers_at_allocated_port_WHEN_findAlive_THEN_origin_and_nothing_foreign() = runTest {
        healthCheck.aliveUrls += "http://localhost:8301"

        assertEquals("http://localhost:8301", resolver.findAlive(webpackHost, options(devServerPort = 8301)))
        assertNull(resolver.findForeign(webpackHost, options(devServerPort = 8301)))
    }

    @Test
    fun GIVEN_nothing_answers_WHEN_findAlive_THEN_null() = runTest {
        ownServerRuns(port = 8086)

        assertNull(resolver.findAlive(kobwebHost, options()))
        assertNull(resolver.findAlive(webpackHost, options(devServerPort = 8301)))
    }

    @Test
    fun GIVEN_module_without_a_known_origin_WHEN_findAlive_or_findForeign_THEN_null() = runTest {
        assertNull(resolver.findAlive(webpackHost, options()))
        assertNull(resolver.findForeign(webpackHost, options()))
    }
}
