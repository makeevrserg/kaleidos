package com.makeevrserg.compose.html.preview.psi

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import org.jetbrains.kotlin.idea.KotlinFileType

/**
 * Candidate files for a scan of the whole project. Every method must be called under a read action.
 *
 * Parsing every Kotlin file of a project would be far too slow for something that runs on every editor
 * event, so the word index of the IDE narrows the candidates down first: only files that mention the
 * annotation at all are parsed.
 */
class KotlinSourceFileFinder(private val projectDependencies: ProjectDependencies) {

    private fun kotlinScope(scope: GlobalSearchScope): GlobalSearchScope {
        return GlobalSearchScope.getScopeRestrictedByFileTypes(scope, KotlinFileType.INSTANCE)
    }

    private fun moduleScope(moduleDirectory: String): GlobalSearchScope {
        val scopes = projectDependencies.moduleManager.modules
            .filter { module -> ExternalSystemApiUtil.getExternalProjectPath(module) == moduleDirectory }
            .map { module -> module.moduleScope }
        if (scopes.isEmpty()) return GlobalSearchScope.EMPTY_SCOPE
        return GlobalSearchScope.union(scopes)
    }

    fun filesMentioning(word: String): List<VirtualFile> {
        val files = mutableListOf<VirtualFile>()
        PsiSearchHelper.getInstance(projectDependencies.project).processCandidateFilesForText(
            kotlinScope(GlobalSearchScope.projectScope(projectDependencies.project)),
            UsageSearchContext.IN_CODE,
            true,
            word
        ) { file ->
            files.add(file)
            true
        }
        return files
    }

    fun filesOf(moduleDirectory: String): List<VirtualFile> {
        return FileTypeIndex.getFiles(KotlinFileType.INSTANCE, kotlinScope(moduleScope(moduleDirectory))).toList()
    }
}
