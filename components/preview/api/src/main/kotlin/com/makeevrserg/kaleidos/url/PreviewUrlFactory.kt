package com.makeevrserg.kaleidos.url

import com.makeevrserg.kaleidos.feature.PreviewFunction
import com.makeevrserg.kaleidos.harness.PreviewPagePath
import com.makeevrserg.kaleidos.host.DevServerKind
import java.net.URLEncoder

/**
 * Address of the generated page: `{origin}{page}?preview={fqn,fqn}#{focusedFqn}`. The page renders every
 * listed preview and scrolls to the fragment; where it lives depends on how the module serves it.
 */
class PreviewUrlFactory {

    private fun String.urlEncoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

    fun create(
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

    private companion object {
        const val PREVIEW_PARAMETER = "preview"
        const val FQN_SEPARATOR = ","
        const val PATH_SEPARATOR = '/'
    }
}
