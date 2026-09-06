package com.makeevrserg.compose.html.preview.feature

import com.makeevrserg.compose.html.preview.host.PreviewHostResolution

/**
 * The file the preview follows: the selected editor file and its preview functions.
 *
 * @param previews every `@Preview` function of the file in source order; empty when the file has none
 * @param focusedFqn preview the page should scroll to, set by the gutter icon
 * @param host module that serves the previews of this file; null until it has been looked up
 */
data class PreviewTarget(
    val filePath: String,
    val fileName: String,
    val previews: List<PreviewFunction>,
    val focusedFqn: String?,
    val host: PreviewHostResolution?
)
