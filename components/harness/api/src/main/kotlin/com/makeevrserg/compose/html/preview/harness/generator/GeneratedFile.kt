package com.makeevrserg.compose.html.preview.harness.generator

/**
 * @param relativePath path inside the generated root of the module it belongs to, with `/` separators
 */
data class GeneratedFile(
    val relativePath: String,
    val content: String
)
