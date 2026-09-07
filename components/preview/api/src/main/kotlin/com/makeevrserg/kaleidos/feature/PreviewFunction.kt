package com.makeevrserg.kaleidos.feature

/**
 * @param fqn fully qualified name of the top-level function, used as the preview identifier in the dev page URL
 * @param filePath path of the containing file; previews are grouped into tool window tabs by it
 */
data class PreviewFunction(
    val fqn: String,
    val name: String,
    val filePath: String,
    val fileName: String
)
