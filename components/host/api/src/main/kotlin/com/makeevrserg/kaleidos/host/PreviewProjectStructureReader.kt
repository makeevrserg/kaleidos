package com.makeevrserg.kaleidos.host

/** Reads the module model of the IDE. Empty until the first Gradle sync of the project has finished. */
interface PreviewProjectStructureReader {
    suspend fun read(): PreviewProjectStructure
}
