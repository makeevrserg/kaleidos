package com.makeevrserg.kaleidos.psi.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.harness.api.ProjectPreviewSource
import com.makeevrserg.kaleidos.psi.annotation.AnnotationFqnResolver
import com.makeevrserg.kaleidos.psi.api.PreviewFunctionDetector
import com.makeevrserg.kaleidos.psi.preview.DefaultPreviewFunctionDetector
import com.makeevrserg.kaleidos.psi.preview.EntryPointDetector
import com.makeevrserg.kaleidos.psi.preview.PreviewFileScanner
import com.makeevrserg.kaleidos.psi.preview.PreviewSourceSetFilter
import com.makeevrserg.kaleidos.psi.source.DefaultSelectedFileScanner
import com.makeevrserg.kaleidos.psi.source.KotlinSourceFileFinder
import com.makeevrserg.kaleidos.psi.source.ProjectPreviewScanner
import com.makeevrserg.kaleidos.source.api.SelectedFileScanner

class PsiModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    val previewFunctionDetector: PreviewFunctionDetector = DefaultPreviewFunctionDetector(AnnotationFqnResolver())

    private val previewFileScanner = PreviewFileScanner(previewFunctionDetector)

    private val previewSourceSetFilter = PreviewSourceSetFilter()

    val selectedFileScanner: SelectedFileScanner = DefaultSelectedFileScanner(
        projectDependencies = intellijCoreModule.projectDependencies,
        fileScanner = previewFileScanner,
        sourceSetFilter = previewSourceSetFilter
    )

    val projectPreviewSource: ProjectPreviewSource = ProjectPreviewScanner(
        projectDependencies = intellijCoreModule.projectDependencies,
        fileFinder = KotlinSourceFileFinder(intellijCoreModule.projectDependencies),
        fileScanner = previewFileScanner,
        sourceSetFilter = previewSourceSetFilter,
        entryPointDetector = EntryPointDetector(),
        backgroundContext = coreModule.dispatchers.default
    )
}
