package com.makeevrserg.kaleidos.host.gradle

import com.intellij.openapi.application.readAction
import com.makeevrserg.kaleidos.host.api.PreviewProjectStructureReader
import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * @param backgroundContext the module model is read under a read action, which must not be requested
 * from the event dispatch thread
 */
internal class IntellijPreviewProjectStructureReader(
    private val gradleModuleCatalog: GradleModuleCatalog,
    private val graphReader: ModuleDependencyGraphReader,
    private val backgroundContext: CoroutineContext
) : PreviewProjectStructureReader {

    override suspend fun read(): PreviewProjectStructure {
        return withContext(backgroundContext) {
            readAction {
                PreviewProjectStructure(modules = gradleModuleCatalog.read(), dependencies = graphReader.read())
            }
        }
    }
}
