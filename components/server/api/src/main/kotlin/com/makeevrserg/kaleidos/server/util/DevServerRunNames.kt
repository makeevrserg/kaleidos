package com.makeevrserg.kaleidos.server.util

import com.makeevrserg.kaleidos.host.model.PreviewHost

internal fun devServerRunName(host: PreviewHost): String = "Kaleidos dev server (${host.displayName})"
