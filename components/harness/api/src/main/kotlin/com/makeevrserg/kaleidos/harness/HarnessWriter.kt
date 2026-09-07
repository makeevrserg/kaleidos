package com.makeevrserg.kaleidos.harness

import com.makeevrserg.kaleidos.harness.generator.GeneratedModuleSources
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Keeps the generated root of a module equal to what was planned. Files whose content did not change
 * are left alone: rewriting them would make the continuous Gradle build recompile the project on every
 * switch between two files.
 *
 * Files that are no longer planned are deleted, otherwise a preview that was renamed or removed would
 * keep the build red until the next `clean`.
 */
class HarnessWriter(private val ioContext: CoroutineContext) {

    private fun existingFiles(root: Path): List<Path> {
        if (!Files.isDirectory(root)) return emptyList()
        return Files.walk(root).use { paths -> paths.filter(Path::isRegularFile).toList() }
    }

    private fun write(file: Path, content: String) {
        if (file.isRegularFile() && runCatching(file::readText).getOrNull() == content) return
        file.createParentDirectories()
        file.writeText(content)
    }

    private fun writeModule(sources: GeneratedModuleSources, root: Path) {
        val planned = sources.files.associateBy { file -> root.resolve(file.relativePath).normalize() }
        existingFiles(root)
            .filterNot { file -> file in planned }
            .forEach { file -> file.deleteIfExists() }
        planned.forEach { (file, source) -> write(file, source.content) }
    }

    suspend fun write(sources: List<GeneratedModuleSources>, layout: HarnessLayout) = withContext(ioContext) {
        sources.forEach { module -> writeModule(module, layout.root(module.moduleDirectory)) }
    }
}
