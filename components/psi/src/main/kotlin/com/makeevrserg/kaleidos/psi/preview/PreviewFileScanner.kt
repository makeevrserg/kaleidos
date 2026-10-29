package com.makeevrserg.kaleidos.psi.preview

import com.intellij.psi.PsiFile
import com.makeevrserg.kaleidos.feature.model.PreviewFunction
import com.makeevrserg.kaleidos.psi.api.PreviewFunctionDetector
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Lists the preview functions of a file in source order. Must be called under a read action.
 */
internal class PreviewFileScanner(
    private val detector: PreviewFunctionDetector
) {
    fun scan(psiFile: PsiFile): List<PreviewFunction> {
        val ktFile = psiFile as? KtFile ?: return emptyList()
        return ktFile.declarations
            .filterIsInstance<KtNamedFunction>()
            .mapNotNull(detector::detect)
    }
}
