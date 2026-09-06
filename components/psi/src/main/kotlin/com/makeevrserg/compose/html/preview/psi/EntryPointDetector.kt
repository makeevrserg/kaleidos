package com.makeevrserg.compose.html.preview.psi

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Finds the `main` of a module. The generated preview application brings its own, so the entry point of
 * the module is left out of the compilation while the preview runs; two of them would not compile.
 */
class EntryPointDetector {

    fun declaresEntryPoint(file: KtFile): Boolean {
        return file.declarations
            .filterIsInstance<KtNamedFunction>()
            .any { function -> function.isTopLevel && function.name == ENTRY_POINT_NAME }
    }

    private companion object {
        const val ENTRY_POINT_NAME = "main"
    }
}
