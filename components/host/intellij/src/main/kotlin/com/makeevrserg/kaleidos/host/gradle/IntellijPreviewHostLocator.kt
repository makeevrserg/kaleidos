package com.makeevrserg.kaleidos.host.gradle

import com.intellij.openapi.application.readAction
import com.makeevrserg.kaleidos.host.api.PreviewHostLocator
import com.makeevrserg.kaleidos.host.api.PreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.model.PreviewHostResolution
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure
import com.makeevrserg.kaleidos.host.usecase.SelectPreviewHostUseCase
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

/**
 * Answers from the module model of the IDE as recorded by the last Gradle sync.
 *
 * @param backgroundContext module model access takes a read action, which must not be requested
 * from the event dispatch thread
 */
internal class IntellijPreviewHostLocator(
    private val structureReader: PreviewProjectStructureReader,
    private val graphReader: ModuleDependencyGraphReader,
    private val selector: SelectPreviewHostUseCase,
    private val backgroundContext: CoroutineContext
) : PreviewHostLocator {

    /**
     * Until the first Gradle sync has populated the module model, every file looks like it belongs to
     * no Gradle module; that case gets its own explanation.
     */
    private fun notFound(filePath: String, structure: PreviewProjectStructure): PreviewHostResolution {
        val reason = if (!structure.isImported) {
            SelectPreviewHostUseCase.GRADLE_NOT_IMPORTED
        } else {
            "${Path.of(filePath).fileName} is not part of a Gradle module"
        }
        return PreviewHostResolution.NotFound(reason)
    }

    override suspend fun locate(filePath: String): PreviewHostResolution {
        val structure = structureReader.read()
        val fileModuleDirectory = withContext(backgroundContext) {
            readAction { graphReader.moduleDirectoryOf(filePath) }
        } ?: return notFound(filePath, structure)
        return selector.invoke(fileModuleDirectory, structure)
    }
}
