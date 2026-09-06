package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution
import kotlinx.coroutines.delay
import kotlin.time.Duration

class FakePreviewHostLocator : PreviewHostLocator {
    val resolutions = mutableMapOf<String, PreviewHostResolution>()

    val locatedFiles = mutableListOf<String>()

    /** How long a lookup takes in virtual time; lets a test interrupt one with a newer request. */
    var locateDelay: Duration = Duration.ZERO

    override suspend fun locate(filePath: String): PreviewHostResolution {
        locatedFiles += filePath
        delay(locateDelay)
        return resolutions[filePath] ?: PreviewHostResolution.NotFound("no host for $filePath")
    }
}
