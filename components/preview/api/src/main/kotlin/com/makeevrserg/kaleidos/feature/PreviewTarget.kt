package com.makeevrserg.kaleidos.feature

import com.makeevrserg.kaleidos.host.PreviewHostResolution

/**
 * The file the preview follows: the selected editor file and what a scan found in it.
 *
 * @param focusedFqn preview the page should scroll to, set by the gutter icon
 * @param host module that serves the previews of this file; null until it has been looked up
 */
data class PreviewTarget(
    val filePath: String,
    val fileName: String,
    val scan: PreviewScan,
    val focusedFqn: String?,
    val host: PreviewHostResolution?
)

/** Previews of the file the generated page can render; empty for every other scan result. */
val PreviewTarget.renderablePreviews: List<PreviewFunction>
    get() = (scan as? PreviewScan.Renderable)?.previews.orEmpty()
