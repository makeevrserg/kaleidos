package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHost

data class EnsureRunningRequest(
    val host: PreviewHost,
    val retryAfterFailure: Boolean
)
