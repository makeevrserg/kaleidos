package com.makeevrserg.compose.html.preview.host

import com.intellij.openapi.application.readAction
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * @param backgroundContext the module model is read under a read action, which must not be requested
 * from the event dispatch thread
 */
class IntellijPreviewProjectStructureReader(
    private val gradleModuleCatalog: GradleModuleCatalog,
    private val graphReader: ModuleDependencyGraphReader,
    private val backgroundContext: CoroutineContext
) : PreviewProjectStructureReader {

    override suspend fun read(): PreviewProjectStructure = withContext(backgroundContext) {
        readAction {
            PreviewProjectStructure(modules = gradleModuleCatalog.read(), dependencies = graphReader.read())
        }
    }
}
