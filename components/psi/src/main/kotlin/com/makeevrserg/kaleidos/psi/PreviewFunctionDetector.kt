package com.makeevrserg.kaleidos.psi

import com.makeevrserg.kaleidos.feature.PreviewFunction
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Recognises functions that can be rendered by the preview page: top-level composables without
 * parameters, receiver or type parameters, annotated with a known `@Preview`.
 */
class PreviewFunctionDetector(
    private val annotationFqnResolver: AnnotationFqnResolver
) {

    private fun KtNamedFunction.hasAnnotationFrom(fqns: Set<String>): Boolean {
        return annotationEntries.any { entry ->
            annotationFqnResolver.resolve(entry).any(fqns::contains)
        }
    }

    private fun KtNamedFunction.isRenderableSignature(): Boolean {
        return isTopLevel &&
            valueParameters.isEmpty() &&
            receiverTypeReference == null &&
            typeParameters.isEmpty()
    }

    fun detect(function: KtNamedFunction): PreviewFunction? {
        if (!function.isRenderableSignature()) return null
        val fqn = function.fqName?.asString() ?: return null
        val name = function.name ?: return null
        val virtualFile = function.containingKtFile.virtualFile ?: return null
        if (!function.hasAnnotationFrom(setOf(PreviewAnnotationCatalog.COMPOSABLE_FQN))) return null
        if (!function.hasAnnotationFrom(PreviewAnnotationCatalog.previewFqns)) return null
        return PreviewFunction(
            fqn = fqn,
            name = name,
            filePath = virtualFile.path,
            fileName = virtualFile.name
        )
    }
}
