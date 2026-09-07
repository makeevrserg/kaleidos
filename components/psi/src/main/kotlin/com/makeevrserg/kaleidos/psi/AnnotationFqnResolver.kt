package com.makeevrserg.kaleidos.psi

import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * Computes the fully qualified names an annotation entry may refer to using only the PSI of the
 * containing file: explicit imports, aliases, star imports and the file package.
 *
 * Resolve is avoided on purpose so the marker works in both K1 and K2 modes of the Kotlin plugin
 * and never triggers a full analysis while the editor is being painted.
 */
class AnnotationFqnResolver {

    private fun KtImportDirective.candidateFor(shortName: String): String? {
        val importedFqName = importedFqName ?: return null
        if (isAllUnder) return "${importedFqName.asString()}.$shortName"
        val visibleName = aliasName ?: importedFqName.shortName().asString()
        return importedFqName.asString().takeIf { visibleName == shortName }
    }

    private fun KtAnnotationEntry.samePackageCandidate(shortName: String): String {
        val packageName = containingKtFile.packageFqName.asString()
        return if (packageName.isEmpty()) shortName else "$packageName.$shortName"
    }

    fun resolve(entry: KtAnnotationEntry): Set<String> {
        val typeText = entry.typeReference?.text ?: return emptySet()
        if (typeText.contains('.')) return setOf(typeText)
        val importedCandidates = entry.containingKtFile.importDirectives
            .mapNotNull { import -> import.candidateFor(typeText) }
        return importedCandidates.toSet() + entry.samePackageCandidate(typeText)
    }
}
