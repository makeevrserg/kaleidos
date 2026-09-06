package com.makeevrserg.compose.html.preview.server

import com.makeevrserg.compose.html.preview.host.PreviewHost

class FakeDetachedServerStopper : DetachedServerStopper {
    val stoppedHosts = mutableListOf<PreviewHost>()

    override suspend fun stop(host: PreviewHost) {
        stoppedHosts += host
    }
}
