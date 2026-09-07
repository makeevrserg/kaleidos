package com.makeevrserg.kaleidos.linemarker

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.makeevrserg.kaleidos.action.ShowPreviewAction
import com.makeevrserg.kaleidos.service.PreviewProjectService
import com.makeevrserg.kaleidos.ui.PreviewIcons
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Puts the preview icon into the gutter of `@Preview @Composable` functions. The marker is anchored
 * to the function name identifier, the smallest PSI leaf, which is what the platform expects to
 * avoid flickering while the file is being highlighted.
 */
class PreviewRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element !is LeafPsiElement || element.elementType != KtTokens.IDENTIFIER) return null
        val function = element.parent as? KtNamedFunction ?: return null
        if (function.nameIdentifier != element) return null
        val detector = element.project.service<PreviewProjectService>().rootModule.psiModule.previewFunctionDetector
        val previewFunction = detector.detect(function) ?: return null
        val actions = arrayOf<AnAction>(ShowPreviewAction(previewFunction))
        return Info(PreviewIcons.Gutter, actions) { "Show preview of ${previewFunction.name}" }
    }
}
