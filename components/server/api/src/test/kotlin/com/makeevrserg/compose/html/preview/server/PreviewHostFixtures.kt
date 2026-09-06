package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.DevServerKind
import com.makeevrserg.compose.html.preview.host.PreviewHost
import java.nio.file.Path

object PreviewHostFixtures {
    fun host(gradlePath: String, kind: DevServerKind, directory: Path? = null): PreviewHost {
        return PreviewHost(
            gradlePath = gradlePath,
            directory = directory?.toString() ?: ("/project" + gradlePath.replace(':', '/')),
            rootProjectPath = "/project",
            kind = kind
        )
    }
}
