package com.makeevrserg.compose.html.preview.server

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/** Writes the files a Kobweb module keeps about its own server, the way Kobweb itself writes them. */
object KobwebServerFixtures {

    fun writeConf(moduleDirectory: Path, port: Int) {
        moduleDirectory.resolve(".kobweb").createDirectories()
        moduleDirectory.resolve(".kobweb/conf.yaml").writeText("server:\n  port: $port\n")
    }

    fun writeServerState(moduleDirectory: Path, port: Int, pid: Long) {
        moduleDirectory.resolve(".kobweb/server").createDirectories()
        moduleDirectory.resolve(".kobweb/server/state.yaml").writeText(
            """
            env: "DEV"
            port: $port
            pid: $pid
            """.trimIndent()
        )
    }
}
