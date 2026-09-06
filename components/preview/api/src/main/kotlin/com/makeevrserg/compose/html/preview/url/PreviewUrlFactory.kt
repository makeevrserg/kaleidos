package com.makeevrserg.compose.html.preview.url

import com.makeevrserg.compose.html.preview.feature.PreviewTarget
import com.makeevrserg.compose.html.preview.harness.PreviewPagePath
import com.makeevrserg.compose.html.preview.host.DevServerKind
import java.net.URLEncoder

/**
 * Address of the generated page: `{origin}{page}?preview={fqn,fqn}#{focusedFqn}`. The page renders every
 * listed preview and scrolls to the fragment; where it lives depends on how the module serves it.
 */
class PreviewUrlFactory {

    private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

    fun create(baseUrl: String, kind: DevServerKind, target: PreviewTarget): String {
        val joinedFqns = target.previews.joinToString(FQN_SEPARATOR) { preview -> preview.fqn.urlEncoded() }
        val origin = baseUrl.trimEnd(PATH_SEPARATOR)
        val pageUrl = "$origin${PreviewPagePath.of(kind)}?$PREVIEW_PARAMETER=$joinedFqns"
        val focusedFqn = target.focusedFqn ?: return pageUrl
        return "$pageUrl#${focusedFqn.urlEncoded()}"
    }

    private companion object {
        const val PREVIEW_PARAMETER = "preview"
        const val FQN_SEPARATOR = ","
        const val PATH_SEPARATOR = '/'
    }
}
