package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.server.PreviewHostFixtures.host
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
        assertEquals("http://localhost:8086", resolver.expected(kobwebHost))
    }

    @Test
    fun GIVEN_webpack_module_never_run_WHEN_expected_THEN_unknown() = runTest {
        assertNull(resolver.expected(webpackHost))
    }

    @Test
    fun GIVEN_origin_remembered_WHEN_expected_THEN_remembered_origin_beats_conf() = runTest {
        resolver.remember(kobwebHost, "http://localhost:9000")

        assertEquals("http://localhost:9000", resolver.expected(kobwebHost))
    }

    @Test
    fun GIVEN_origin_forgotten_WHEN_expected_THEN_falls_back_to_conf() = runTest {
        resolver.remember(kobwebHost, "http://localhost:9000")
        resolver.forget(kobwebHost)

        assertEquals("http://localhost:8086", resolver.expected(kobwebHost))
    }

    @Test
    fun GIVEN_remembered_origins_WHEN_expected_THEN_kept_per_module() = runTest {
        resolver.remember(webpackHost, "http://localhost:8085")
        resolver.remember(kobwebHost, "http://localhost:8086")

        assertEquals("http://localhost:8085", resolver.expected(webpackHost))
        assertEquals("http://localhost:8086", resolver.expected(kobwebHost))
    }

    @Test
    fun GIVEN_server_answers_at_expected_origin_WHEN_findAlive_THEN_origin() = runTest {
        healthCheck.aliveUrls += "http://localhost:8086"

        assertEquals("http://localhost:8086", resolver.findAlive(kobwebHost))
    }

    @Test
    fun GIVEN_nothing_answers_WHEN_findAlive_THEN_null() = runTest {
        assertNull(resolver.findAlive(kobwebHost))
        assertNull(resolver.findAlive(webpackHost))
    }
}
