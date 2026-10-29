package com.makeevrserg.kaleidos.source.api

import com.makeevrserg.kaleidos.feature.model.PreviewScan

interface SelectedFileScanner {
    fun scan(filePath: String): PreviewScan
}
