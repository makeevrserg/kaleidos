package com.makeevrserg.compose.html.preview.url

import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import java.net.URLEncoder

/**
 * Page contract shared with the preview module: `{origin}/?preview={fqn,fqn}#{focusedFqn}`. The page
 * renders every listed preview and scrolls to the fragment.
 */
class PreviewUrlFactory {

    private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

    fun create(baseUrl: String, target: PreviewTarget): String {
        val joinedFqns = target.previews.joinToString(FQN_SEPARATOR) { preview -> preview.fqn.urlEncoded() }
        val pageUrl = "${baseUrl.trimEnd(PATH_SEPARATOR)}/?$PREVIEW_PARAMETER=$joinedFqns"
        val focusedFqn = target.focusedFqn ?: return pageUrl
        return "$pageUrl#${focusedFqn.urlEncoded()}"
    }

    private companion object {
        const val PREVIEW_PARAMETER = "preview"
        const val FQN_SEPARATOR = ","
        const val PATH_SEPARATOR = '/'
    }
}
