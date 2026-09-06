package com.makeevrserg.compose.html.preview.psi

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.compose.html.preview.dependencies.ProjectDependencies
import com.makeevrserg.compose.html.preview.harness.HarnessPreview
import com.makeevrserg.compose.html.preview.harness.ModulePreviews
import com.makeevrserg.compose.html.preview.harness.ProjectPreviewSource
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.psi.KtFile
import kotlin.coroutines.CoroutineContext

/**
 * The previews of the whole project, the way the generated registries need them: grouped by the Gradle
 * module that compiles them. Only source sets that end up in the Kotlin/JS compilation are looked at,
 * and tests are skipped: their code is not part of the application the dev server serves.
 *
 * @param backgroundContext the scan takes a read action and waits for indexing, neither of which may
 * happen on the event dispatch thread
 */
class ProjectPreviewScanner(
    private val projectDependencies: ProjectDependencies,
    private val fileFinder: KotlinSourceFileFinder,
    private val fileScanner: PreviewFileScanner,
    private val sourceSetFilter: PreviewSourceSetFilter,
    private val entryPointDetector: EntryPointDetector,
    private val backgroundContext: CoroutineContext
) : ProjectPreviewSource {

    /** Null for files outside a Gradle module, in a test source set or in a source set of another platform. */
    private fun moduleDirectoryOf(file: VirtualFile): String? {
        if (projectDependencies.fileIndex.isInTestSourceContent(file)) return null
        val module = projectDependencies.fileIndex.getModuleForFile(file) ?: return null
        if (!sourceSetFilter.isPreviewSource(module)) return null
        return ExternalSystemApiUtil.getExternalProjectPath(module)
    }

    private fun ktFile(file: VirtualFile): KtFile? {
        if (!file.isValid) return null
        return projectDependencies.psiManager.findFile(file) as? KtFile
    }

    private fun previewsOf(file: VirtualFile): List<HarnessPreview> {
        val ktFile = ktFile(file) ?: return emptyList()
        return fileScanner.scan(ktFile).map { preview -> HarnessPreview(preview.fqn) }
    }

    private fun sourceRootRelativePath(file: VirtualFile): String? {
        val sourceRoot = projectDependencies.fileIndex.getSourceRootForFile(file) ?: return null
        return VfsUtilCore.getRelativePath(file, sourceRoot, PATH_SEPARATOR)
    }

    override suspend fun previews(): List<ModulePreviews> = withContext(backgroundContext) {
        smartReadAction(projectDependencies.project) {
            fileFinder.filesMentioning(PREVIEW_WORD)
                .mapNotNull { file -> moduleDirectoryOf(file)?.let { directory -> directory to previewsOf(file) } }
                .groupBy(keySelector = { entry -> entry.first }, valueTransform = { entry -> entry.second })
                .map { entry -> ModulePreviews(moduleDirectory = entry.key, previews = entry.value.flatten()) }
        }
    }

    override suspend fun entryPointSourcePaths(moduleDirectory: String): List<String> =
        withContext(backgroundContext) {
            smartReadAction(projectDependencies.project) {
                fileFinder.filesOf(moduleDirectory)
                    .filter { file -> moduleDirectoryOf(file) == moduleDirectory }
                    .filter { file -> ktFile(file)?.let(entryPointDetector::declaresEntryPoint) == true }
                    .mapNotNull(::sourceRootRelativePath)
            }
        }

    private companion object {
        /** Short name of every supported preview annotation, the word the index is asked for. */
        const val PREVIEW_WORD = "Preview"
        const val PATH_SEPARATOR = '/'
    }
}
