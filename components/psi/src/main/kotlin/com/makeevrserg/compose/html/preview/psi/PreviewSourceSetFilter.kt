package com.makeevrserg.compose.html.preview.psi

import com.intellij.openapi.module.Module

/**
 * Source sets whose code the generated registry can call. The registry is compiled into the Kotlin/JS
 * compilation of the module, so a preview written for another platform, `jvmMain` for example, is not
 * a preview this plugin can render and must not reach the generated sources.
 *
 * The IDE imports every source set of a Gradle module as a module of its own, named after it
 * (`project.components.ui.jsMain`), which is what the source set is read from here.
 */
class PreviewSourceSetFilter {

    fun isPreviewSource(module: Module): Boolean = module.name.substringAfterLast(NAME_SEPARATOR) in PREVIEW_SOURCE_SETS

    private companion object {
        const val NAME_SEPARATOR = '.'
        val PREVIEW_SOURCE_SETS = setOf("commonMain", "jsMain", "main")
    }
}
