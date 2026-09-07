package com.makeevrserg.kaleidos.host

sealed interface PreviewHostResolution {
    data class Found(val host: PreviewHost) : PreviewHostResolution

    /** @param reason worded for the user; shown in the tool window and as a notification */
    data class NotFound(val reason: String) : PreviewHostResolution
}
