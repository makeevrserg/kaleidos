package com.makeevrserg.kaleidos.feature

/**
 * Source sets whose code the generated registry can call. The registry is compiled into the Kotlin/JS
 * compilation of the module, so a preview written for another platform, `jvmMain` for example, never
 * reaches the page. Shared by the scan that decides it and by the wording that explains it.
 */
object PreviewSourceSetCatalog {
    val previewSourceSets: Set<String> = setOf("commonMain", "jsMain", "main")
}
