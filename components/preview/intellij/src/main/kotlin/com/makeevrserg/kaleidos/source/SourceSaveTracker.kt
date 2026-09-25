package com.makeevrserg.kaleidos.source

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.makeevrserg.kaleidos.dependencies.ProjectDependencies
import com.makeevrserg.kaleidos.feature.PreviewStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SourceSaveTracker(
    private val projectDependencies: ProjectDependencies,
    private val contract: PreviewStore
) {
    private fun VFileEvent.isProjectKotlinSourceSave(): Boolean {
        if (this !is VFileContentChangeEvent) return false
        return file.extension == KOTLIN_EXTENSION && projectDependencies.fileIndex.isInContent(file)
    }

    private fun saves(): Flow<Unit> = callbackFlow {
        val listener = object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (events.any { event -> event.isProjectKotlinSourceSave() }) trySend(Unit)
            }
        }
        val connection = projectDependencies.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, listener)
        awaitClose { connection.disconnect() }
    }

    suspend fun track() = saves().collect { contract.onSourcesSaved() }

    private companion object {
        const val KOTLIN_EXTENSION = "kt"
    }
}
