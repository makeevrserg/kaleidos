package com.makeevrserg.kaleidos.source.api

import com.makeevrserg.kaleidos.source.model.ScanRequest
import kotlinx.coroutines.flow.Flow

interface ScanScheduler {
    fun <File : Any> scanRequests(selectedFiles: Flow<File?>, editedFiles: Flow<File>): Flow<ScanRequest<File>>
}
