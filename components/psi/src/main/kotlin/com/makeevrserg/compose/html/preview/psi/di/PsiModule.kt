package com.makeevrserg.compose.html.preview.psi.di

import com.makeevrserg.compose.html.preview.psi.AnnotationFqnResolver
import com.makeevrserg.compose.html.preview.psi.PreviewFileScanner
import com.makeevrserg.compose.html.preview.psi.PreviewFunctionDetector

class PsiModule {
    val previewFunctionDetector = PreviewFunctionDetector(AnnotationFqnResolver())

    val previewFileScanner = PreviewFileScanner(previewFunctionDetector)
}
