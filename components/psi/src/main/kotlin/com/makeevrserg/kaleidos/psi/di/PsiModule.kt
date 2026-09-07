package com.makeevrserg.kaleidos.psi.di

import com.makeevrserg.kaleidos.core.di.CoreModule
import com.makeevrserg.kaleidos.core.di.IntellijCoreModule
import com.makeevrserg.kaleidos.harness.ProjectPreviewSource
import com.makeevrserg.kaleidos.psi.AnnotationFqnResolver
import com.makeevrserg.kaleidos.psi.EntryPointDetector
import com.makeevrserg.kaleidos.psi.KotlinSourceFileFinder
import com.makeevrserg.kaleidos.psi.PreviewFileScanner
import com.makeevrserg.kaleidos.psi.PreviewFunctionDetector
import com.makeevrserg.kaleidos.psi.PreviewSourceSetFilter
import com.makeevrserg.kaleidos.psi.ProjectPreviewScanner
import com.makeevrserg.kaleidos.psi.SelectedFileScanner

class PsiModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    val previewFunctionDetector = PreviewFunctionDetector(AnnotationFqnResolver())

    private val previewFileScanner = PreviewFileScanner(previewFunctionDetector)

    private val previewSourceSetFilter = PreviewSourceSetFilter()

    val selectedFileScanner = SelectedFileScanner(
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
