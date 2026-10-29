package com.makeevrserg.kaleidos.psi.source

import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.model.PreviewScan
import com.makeevrserg.kaleidos.psi.preview.PreviewFileScanner
import com.makeevrserg.kaleidos.psi.preview.PreviewSourceSetFilter
import com.makeevrserg.kaleidos.source.api.SelectedFileScanner
import java.nio.file.Path

/**
 * What the tool window can show for one editor file. Must be called under a read action.
 *
 * A file outside every Gradle module keeps [PreviewScan.Renderable]: the module lookup that follows
 * explains that case far better than a source set name nobody imported would.
 */
internal class DefaultSelectedFileScanner(
    private val projectDependencies: ProjectDependencies,
    private val fileScanner: PreviewFileScanner,
    private val sourceSetFilter: PreviewSourceSetFilter
) : SelectedFileScanner {

    /** A file deleted between the editor event and the scan is invalid; PSI lookup would log an error for it. */
    override fun scan(filePath: String): PreviewScan {
        val file = projectDependencies.virtualFileManager.findFileByNioPath(Path.of(filePath))
            ?: return PreviewScan.NoPreviews
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
