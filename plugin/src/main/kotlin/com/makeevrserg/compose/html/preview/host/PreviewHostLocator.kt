package com.makeevrserg.compose.html.preview.host

import com.intellij.openapi.application.readAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.file.Path

/**
 * Finds the module that serves the previews of a file from the module model of the IDE. No settings
 * are involved: the answer follows the Gradle structure of the project.
 */
class PreviewHostLocator(
    private val gradleModuleCatalog: GradleModuleCatalog,
    private val graphReader: ModuleDependencyGraphReader,
    private val selector: PreviewHostSelector,
    private val backgroundDispatcher: CoroutineDispatcher
) {

    /**
     * Until the first Gradle sync has populated the module model, every file looks like it belongs to
     * no Gradle module; that case gets its own explanation.
     */
    private fun notFound(filePath: String, graph: ModuleDependencyGraph): PreviewHostResolution {
        val reason = if (graph.dependencies.isEmpty()) {
            PreviewHostSelector.GRADLE_NOT_IMPORTED
        } else {
            "${Path.of(filePath).fileName} is not part of a Gradle module"
        }
        return PreviewHostResolution.NotFound(reason)
    }

    /** Module model access takes a read action, which must not be requested from the event dispatch thread. */
    suspend fun locate(filePath: String): PreviewHostResolution = withContext(backgroundDispatcher) {
        readAction {
            val graph = graphReader.read()
            val fileModuleDirectory = graphReader.moduleDirectoryOf(filePath)
            if (fileModuleDirectory == null) {
                notFound(filePath, graph)
            } else {
                selector.select(fileModuleDirectory, gradleModuleCatalog.read(), graph)
            }
        }
    }
}
