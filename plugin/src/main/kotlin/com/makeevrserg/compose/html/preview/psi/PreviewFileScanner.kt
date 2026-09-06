package com.makeevrserg.compose.html.preview.psi

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Lists the preview functions of a file in source order. Must be called under a read action.
 */
class PreviewFileScanner(
    private val detector: PreviewFunctionDetector
) {
    fun scan(psiFile: PsiFile): List<PreviewFunction> {
        val ktFile = psiFile as? KtFile ?: return emptyList()
        return ktFile.declarations
            .filterIsInstance<KtNamedFunction>()
            .mapNotNull(detector::detect)
    }
}
