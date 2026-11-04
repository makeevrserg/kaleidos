package com.makeevrserg.kaleidos.preview.api.source.api

import com.makeevrserg.kaleidos.preview.api.feature.model.PreviewScan

interface SelectedFileScanner {
    fun scan(filePath: String): PreviewScan
}
