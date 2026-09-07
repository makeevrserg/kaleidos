package com.makeevrserg.kaleidos.psi

import com.intellij.openapi.vfs.VirtualFile
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.PreviewScan

/**
 * What the tool window can show for one editor file. Must be called under a read action.
 *
 * A file outside every Gradle module keeps [PreviewScan.Renderable]: the module lookup that follows
 * explains that case far better than a source set name nobody imported would.
 */
class SelectedFileScanner(
    private val projectDependencies: ProjectDependencies,
    private val fileScanner: PreviewFileScanner,
    private val sourceSetFilter: PreviewSourceSetFilter
) {

    /** A file deleted between the editor event and the scan is invalid; PSI lookup would log an error for it. */
    fun scan(file: VirtualFile): PreviewScan {
        if (!file.isValid) return PreviewScan.NoPreviews
        val psiFile = projectDependencies.psiManager.findFile(file) ?: return PreviewScan.NoPreviews
        val previews = fileScanner.scan(psiFile)
        if (previews.isEmpty()) return PreviewScan.NoPreviews
        val module = projectDependencies.fileIndex.getModuleForFile(file)
        if (module != null && !sourceSetFilter.isPreviewSource(module)) {
            return PreviewScan.UnsupportedSourceSet(sourceSetFilter.sourceSetName(module))
        }
        return PreviewScan.Renderable(previews)
    }
}
