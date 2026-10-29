package com.makeevrserg.kaleidos.psi.api

import com.makeevrserg.kaleidos.feature.model.PreviewFunction
import org.jetbrains.kotlin.psi.KtNamedFunction

interface PreviewFunctionDetector {
    fun detect(function: KtNamedFunction): PreviewFunction?
}
