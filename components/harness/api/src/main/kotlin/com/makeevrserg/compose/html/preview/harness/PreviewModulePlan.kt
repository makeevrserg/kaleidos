package com.makeevrserg.compose.html.preview.harness

/**
 * One module that gets a generated registry, so that its previews, `internal` ones included, can be
 * called from the page of the host module.
 */
data class PreviewModulePlan(
    val gradlePath: String,
    val directory: String,
    val previews: List<HarnessPreview>
)
