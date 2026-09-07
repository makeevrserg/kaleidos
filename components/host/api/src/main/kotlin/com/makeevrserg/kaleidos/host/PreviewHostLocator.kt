package com.makeevrserg.kaleidos.host

/**
 * Finds the module that serves the previews of a file. No settings are involved: the answer follows
 * the Gradle structure of the project.
 */
interface PreviewHostLocator {
    suspend fun locate(filePath: String): PreviewHostResolution
}
