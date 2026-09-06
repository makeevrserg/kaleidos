package com.makeevrserg.compose.html.preview.feature

/**
 * A request to look up the preview module of [target] and bring its dev server up.
 *
 * @param attempt distinguishes repeated requests for the same target, so each one is processed
 */
data class PreviewConnectRequest(
    val target: PreviewTarget,
    val retryAfterFailure: Boolean,
    val attempt: Int
)
