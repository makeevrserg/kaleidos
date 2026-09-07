package com.makeevrserg.kaleidos.server

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KobwebConfReaderTest {
    private val moduleDirectory: Path = Files.createTempDirectory("kobweb-module")

    private val reader = KobwebConfReader(ioContext = kotlin.coroutines.EmptyCoroutineContext)

    private fun writeConf(content: String) {
        moduleDirectory.resolve(".kobweb").createDirectories()
        moduleDirectory.resolve(".kobweb/conf.yaml").writeText(content)
    }

    @AfterTest
    fun cleanUp() {
        moduleDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_conf_with_port_WHEN_readBaseUrl_THEN_origin_uses_that_port() = runTest {
        writeConf(
            """
            site:
              title: "Preview"
            server:
              port: 8086
            """.trimIndent()
        )

        assertEquals("http://localhost:8086", reader.readBaseUrl(moduleDirectory.toString()))
    }

    @Test
    fun GIVEN_no_conf_file_WHEN_readBaseUrl_THEN_kobweb_default_port() = runTest {
        assertEquals("http://localhost:8080", reader.readBaseUrl(moduleDirectory.toString()))
    }

    @Test
    fun GIVEN_conf_without_port_WHEN_readBaseUrl_THEN_kobweb_default_port() = runTest {
        writeConf(
            """
            site:
              title: "Preview"
            """.trimIndent()
        )

        assertEquals("http://localhost:8080", reader.readBaseUrl(moduleDirectory.toString()))
    }

    @Test
    fun GIVEN_port_mentioned_inside_a_value_WHEN_readBaseUrl_THEN_not_mistaken_for_the_setting() = runTest {
        writeConf(
            """
            site:
              title: "port: 1234"
            """.trimIndent()
        )

        assertEquals("http://localhost:8080", reader.readBaseUrl(moduleDirectory.toString()))
    }
}
