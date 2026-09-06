package com.makeevrserg.compose.html.preview.psi.di

import com.makeevrserg.compose.html.preview.core.di.CoreModule
import com.makeevrserg.compose.html.preview.core.di.IntellijCoreModule
import com.makeevrserg.compose.html.preview.harness.ProjectPreviewSource
import com.makeevrserg.compose.html.preview.psi.AnnotationFqnResolver
import com.makeevrserg.compose.html.preview.psi.EntryPointDetector
import com.makeevrserg.compose.html.preview.psi.KotlinSourceFileFinder
import com.makeevrserg.compose.html.preview.psi.PreviewFileScanner
import com.makeevrserg.compose.html.preview.psi.PreviewFunctionDetector
import com.makeevrserg.compose.html.preview.psi.PreviewSourceSetFilter
import com.makeevrserg.compose.html.preview.psi.ProjectPreviewScanner

class PsiModule(
    coreModule: CoreModule,
    intellijCoreModule: IntellijCoreModule
) {
    val previewFunctionDetector = PreviewFunctionDetector(AnnotationFqnResolver())

    val previewFileScanner = PreviewFileScanner(previewFunctionDetector)

    val projectPreviewSource: ProjectPreviewSource = ProjectPreviewScanner(
        projectDependencies = intellijCoreModule.projectDependencies,
        fileFinder = KotlinSourceFileFinder(intellijCoreModule.projectDependencies),
        fileScanner = previewFileScanner,
        sourceSetFilter = PreviewSourceSetFilter(),
        entryPointDetector = EntryPointDetector(),
        backgroundContext = coreModule.dispatchers.default
    )
}
