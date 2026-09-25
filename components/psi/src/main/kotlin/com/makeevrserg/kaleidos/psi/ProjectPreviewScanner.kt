package com.makeevrserg.kaleidos.psi

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.harness.HarnessPreview
import com.makeevrserg.kaleidos.harness.ModulePreviews
import com.makeevrserg.kaleidos.harness.PrivatePreviewFile
import com.makeevrserg.kaleidos.harness.ProjectPreviewSource
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

    private fun sourceRootRelativePath(file: VirtualFile): String? {
        val sourceRoot = projectDependencies.fileIndex.getSourceRootForFile(file) ?: return null
        return VfsUtilCore.getRelativePath(file, sourceRoot, PATH_SEPARATOR)
    }

    private fun scan(file: VirtualFile): ScannedPreviewFile? {
        val moduleDirectory = moduleDirectoryOf(file) ?: return null
        val ktFile = ktFile(file) ?: return null
        val sourcePath = sourceRootRelativePath(file) ?: return null
        val previews = fileScanner.scan(ktFile).map { preview ->
            HarnessPreview(fqn = preview.fqn, sourcePath = sourcePath, isPrivate = preview.isPrivate)
        }
        val hasPrivatePreviews = previews.any { preview -> preview.isPrivate }
        return ScannedPreviewFile(
            moduleDirectory = moduleDirectory,
            previews = previews,
            privatePreviewFile = if (hasPrivatePreviews) PrivatePreviewFile(sourcePath, ktFile.text) else null
        )
    }

    private fun List<ScannedPreviewFile>.toModulePreviews(moduleDirectory: String): ModulePreviews {
        return ModulePreviews(
            moduleDirectory = moduleDirectory,
            previews = flatMap { file -> file.previews },
            privatePreviewFiles = mapNotNull { file -> file.privatePreviewFile }
        )
    }

    override suspend fun previews(): List<ModulePreviews> = withContext(backgroundContext) {
        smartReadAction(projectDependencies.project) {
            fileFinder.filesMentioning(PREVIEW_WORD)
                .mapNotNull(::scan)
                .groupBy { file -> file.moduleDirectory }
                .map { entry -> entry.value.toModulePreviews(entry.key) }
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
