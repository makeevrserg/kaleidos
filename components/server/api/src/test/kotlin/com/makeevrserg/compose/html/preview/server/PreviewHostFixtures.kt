package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import java.nio.file.Path

object PreviewHostFixtures {
    const val INIT_SCRIPT_PATH = "/project/build/compose-html-preview/compose-html-preview.init.gradle"

    fun host(gradlePath: String, kind: DevServerKind, directory: Path? = null): PreviewHost {
        return PreviewHost(
            gradlePath = gradlePath,
            directory = directory?.toString() ?: ("/project" + gradlePath.replace(':', '/')),
            rootProjectPath = "/project",
            kind = kind
        )
    }

    fun options(devServerPort: Int? = null): DevServerLaunchOptions {
        return DevServerLaunchOptions(initScriptPath = INIT_SCRIPT_PATH, devServerPort = devServerPort)
    }
}
