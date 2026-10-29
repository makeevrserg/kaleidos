package com.makeevrserg.kaleidos.url.util

import com.makeevrserg.kaleidos.feature.model.PreviewFunction
import com.makeevrserg.kaleidos.harness.util.previewPagePath
import com.makeevrserg.kaleidos.host.model.DevServerKind

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
    val pageUrl = "$origin${previewPagePath(kind)}?$PREVIEW_PARAMETER=$joinedFqns"
    if (focusedFqn == null) return pageUrl
    return "$pageUrl#${focusedFqn.urlEncoded()}"
}
