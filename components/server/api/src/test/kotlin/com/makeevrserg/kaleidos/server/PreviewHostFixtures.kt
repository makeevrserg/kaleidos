package com.makeevrserg.kaleidos.server

import com.makeevrserg.kaleidos.host.DevServerKind
import com.makeevrserg.kaleidos.host.PreviewHost
import java.nio.file.Path

object PreviewHostFixtures {
    const val INIT_SCRIPT_PATH = "/project/build/kaleidos/kaleidos.init.gradle"

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
