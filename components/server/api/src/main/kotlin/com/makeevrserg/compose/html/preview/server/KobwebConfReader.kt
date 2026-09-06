package com.makeevrserg.compose.html.preview.server

import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * The Kobweb server takes its port from `.kobweb/conf.yaml` and outlives the Gradle run, so the
 * origin is known before the task starts and a server left from a previous IDE session is adopted.
 */
class KobwebConfReader(private val ioContext: CoroutineContext) {

    private fun parsePort(conf: String): Int? = PORT_PATTERN.find(conf)?.groupValues?.get(PORT_GROUP)?.toIntOrNull()

    /** A missing file or port means Kobweb's own default, exactly as the Kobweb server itself behaves. */
    suspend fun readBaseUrl(moduleDirectory: String): String = withContext(ioContext) {
        val conf = Path.of(moduleDirectory, KOBWEB_DIRECTORY, CONF_FILE_NAME)
        val port = runCatching { Files.readString(conf) }.map(::parsePort).getOrNull() ?: DEFAULT_PORT
        "http://localhost:$port"
    }

    private companion object {
        const val KOBWEB_DIRECTORY = ".kobweb"
        const val CONF_FILE_NAME = "conf.yaml"
        const val DEFAULT_PORT = 8080
        const val PORT_GROUP = 1
        val PORT_PATTERN = Regex("""(?m)^\s*port:\s*(\d+)\s*$""")
    }
}
