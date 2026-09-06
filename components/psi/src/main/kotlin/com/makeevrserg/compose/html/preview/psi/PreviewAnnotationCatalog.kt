package com.makeevrserg.compose.html.preview.psi

/**
 * Annotations recognised by the gutter marker. Both the unified `androidx` annotation shipped with
 * Compose Multiplatform 1.10+ and the older JetBrains and Desktop variants are accepted, so existing
 * code does not have to be migrated.
 */
object PreviewAnnotationCatalog {
    const val COMPOSABLE_FQN = "androidx.compose.runtime.Composable"

    val previewFqns: Set<String> = setOf(
        "androidx.compose.ui.tooling.preview.Preview",
        "org.jetbrains.compose.ui.tooling.preview.Preview",
        "androidx.compose.desktop.ui.tooling.preview.Preview"
    )
}
