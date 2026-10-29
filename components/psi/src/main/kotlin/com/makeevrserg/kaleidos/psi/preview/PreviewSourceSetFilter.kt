package com.makeevrserg.kaleidos.psi.preview

import com.intellij.openapi.module.Module
import com.makeevrserg.kaleidos.feature.util.PREVIEW_SOURCE_SETS

/**
 * Reads the source set of an IDE module and answers whether the generated registry may call its code.
 *
 * The IDE imports every source set of a Gradle module as a module of its own, named after it
 * (`project.components.ui.jsMain`), which is what the source set is read from here. Which of them
 * count is [PreviewSourceSetCatalog].
 */
internal class PreviewSourceSetFilter {

    fun sourceSetName(module: Module): String = module.name.substringAfterLast(NAME_SEPARATOR)

    fun isPreviewSource(module: Module): Boolean {
        return sourceSetName(module) in PREVIEW_SOURCE_SETS
    }

    private companion object {
        const val NAME_SEPARATOR = '.'
    }
}
