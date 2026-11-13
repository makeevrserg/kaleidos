package com.makeevrserg.kaleidos.preview.api.url.util

import com.makeevrserg.kaleidos.harness.api.util.previewPagePath
import com.makeevrserg.kaleidos.host.api.model.DevServerKind
import com.makeevrserg.kaleidos.preview.api.feature.model.PreviewFunction

private const val PREVIEW_PARAMETER = "preview"
private const val FQN_SEPARATOR = ","
private const val PATH_SEPARATOR = '/'

internal fun DevServerKind.buildPreviewUrl(
    baseUrl: String,
    previews: List<PreviewFunction>,
    focusedFqn: String?
): String {
    val joinedFqns = previews.joinToString(FQN_SEPARATOR) { preview -> preview.fqn.urlEncoded() }
    val origin = baseUrl.trimEnd(PATH_SEPARATOR)
    val pageUrl = "$origin${previewPagePath()}?$PREVIEW_PARAMETER=$joinedFqns"
    if (focusedFqn == null) return pageUrl
    return "$pageUrl#${focusedFqn.urlEncoded()}"
}
