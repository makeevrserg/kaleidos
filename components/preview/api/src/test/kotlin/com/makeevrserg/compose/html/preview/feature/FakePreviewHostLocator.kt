package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHostLocator
import com.makeevrserg.compose.html.preview.host.PreviewHostResolution

class FakePreviewHostLocator : PreviewHostLocator {
    val resolutions = mutableMapOf<String, PreviewHostResolution>()

    val locatedFiles = mutableListOf<String>()

    override suspend fun locate(filePath: String): PreviewHostResolution {
        locatedFiles += filePath
        return resolutions[filePath] ?: PreviewHostResolution.NotFound("no host for $filePath")
    }
}
