package com.makeevrserg.kaleidos.url.util

import com.makeevrserg.kaleidos.feature.PreviewFunction
import com.makeevrserg.kaleidos.harness.PreviewPagePath
import com.makeevrserg.kaleidos.host.DevServerKind

private const val PREVIEW_PARAMETER = "preview"
private const val FQN_SEPARATOR = ","
private const val PATH_SEPARATOR = '/'

internal fun buildPreviewUrl(
    baseUrl: String,
    kind: DevServerKind,
    previews: List<PreviewFunction>,
    focusedFqn: String?
): String {
    val joinedFqns = previews.joinToString(FQN_SEPARATOR) { preview -> preview.fqn.urlEncoded() }
    val origin = baseUrl.trimEnd(PATH_SEPARATOR)
    val pageUrl = "$origin${PreviewPagePath.of(kind)}?$PREVIEW_PARAMETER=$joinedFqns"
    if (focusedFqn == null) return pageUrl
    return "$pageUrl#${focusedFqn.urlEncoded()}"
}
