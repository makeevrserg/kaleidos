@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.server.file

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultKobwebConfReaderTest {
    private val moduleDirectory: Path = Files.createTempDirectory("kobweb-module")

    private val reader = DefaultKobwebConfReader(ioContext = kotlin.coroutines.EmptyCoroutineContext)

    private fun writeConf(content: String) {
        moduleDirectory.resolve(".kobweb").createDirectories()
        moduleDirectory.resolve(".kobweb/conf.yaml").writeText(content)
    }

    @AfterTest
    fun cleanUp() {
        moduleDirectory.toFile().deleteRecursively()
    }

    @Test
    fun GIVEN_conf_with_port_WHEN_read_port_THEN_origin_uses_that_port() {
        return runTest {
            writeConf(
                """
            site:
              title: "Preview"
            server:
              port: 8086
                """.trimIndent()
            )

            assertEquals(8086, reader.readPort(moduleDirectory.toString()))
        }
    }

    @Test
    fun GIVEN_no_conf_file_WHEN_read_port_THEN_kobweb_default_port() {
        return runTest {
            assertEquals(8080, reader.readPort(moduleDirectory.toString()))
        }
    }

    @Test
    fun GIVEN_conf_without_port_WHEN_read_port_THEN_kobweb_default_port() {
        return runTest {
            writeConf(
                """
            site:
              title: "Preview"
                """.trimIndent()
            )

            assertEquals(8080, reader.readPort(moduleDirectory.toString()))
        }
    }

    @Test
    fun GIVEN_port_mentioned_inside_a_value_WHEN_read_port_THEN_not_mistaken_for_the_setting() {
        return runTest {
            writeConf(
                """
            site:
              title: "port: 1234"
                """.trimIndent()
            )

            assertEquals(8080, reader.readPort(moduleDirectory.toString()))
        }
    }
}
