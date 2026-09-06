package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.options
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DevServerOriginResolverTest {
    private val kobwebDirectory: Path = Files.createTempDirectory("kobweb-module").also { directory ->
        directory.resolve(".kobweb").createDirectories()
        directory.resolve(".kobweb/conf.yaml").writeText("server:\n  port: 8086\n")
    }

    private val kobwebHost = host(":instances:web-preview-kobweb", DevServerKind.KOBWEB, kobwebDirectory)

    private val webpackHost = host(":instances:web-preview", DevServerKind.WEBPACK)

    private val healthCheck = FakeDevServerHealthCheck()

    private val resolver = DevServerOriginResolver(
        kobwebConfReader = KobwebConfReader(ioContext = EmptyCoroutineContext),
        healthCheck = healthCheck
    )

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
    fun GIVEN_server_answers_at_expected_origin_WHEN_findAlive_THEN_origin() = runTest {
        healthCheck.aliveUrls += "http://localhost:8086"

        assertEquals("http://localhost:8086", resolver.findAlive(kobwebHost, options()))
    }

    @Test
    fun GIVEN_nothing_answers_WHEN_findAlive_THEN_null() = runTest {
        assertNull(resolver.findAlive(kobwebHost, options()))
        assertNull(resolver.findAlive(webpackHost, options(devServerPort = 8301)))
    }
}
