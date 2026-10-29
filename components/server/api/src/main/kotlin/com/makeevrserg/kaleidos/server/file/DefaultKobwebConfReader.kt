package com.makeevrserg.kaleidos.server.file

import com.makeevrserg.kaleidos.core.coroutine.util.propagateCancellationException
import com.makeevrserg.kaleidos.server.api.KobwebConfReader
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * The Kobweb server takes its port from `.kobweb/conf.yaml` and outlives the Gradle run, so the
 * origin is known before the task starts and a server left from a previous IDE session can be
 * recognised there.
 */
internal class DefaultKobwebConfReader(private val ioContext: CoroutineContext) : KobwebConfReader {

    private fun parsePort(conf: String): Int? = PORT_PATTERN.find(conf)?.groupValues?.get(PORT_GROUP)?.toIntOrNull()

    /** A missing file or port means Kobweb's own default, exactly as the Kobweb server itself behaves. */
    override suspend fun readPort(moduleDirectory: String): Int {
        return withContext(ioContext) {
            val conf = Path.of(
                moduleDirectory,
                KOBWEB_DIRECTORY,
                CONF_FILE_NAME
            )
            runCatching { Files.readString(conf) }
                .propagateCancellationException()
                .map(::parsePort)
                .getOrNull() ?: DEFAULT_PORT
        }
    }

    private companion object {
        const val KOBWEB_DIRECTORY = ".kobweb"
        const val CONF_FILE_NAME = "conf.yaml"
        const val DEFAULT_PORT = 8080
        const val PORT_GROUP = 1
        val PORT_PATTERN = Regex("""(?m)^\s*port:\s*(\d+)\s*$""")
    }
}
