package com.makeevrserg.kaleidos.harness

import com.makeevrserg.kaleidos.harness.generator.GeneratedFile
import com.makeevrserg.kaleidos.harness.generator.GeneratedModuleSources
import kotlinx.coroutines.test.runTest
import java.nio.file.attribute.FileTime
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HarnessWriterTest {
    private val layout = HarnessLayout()

    private val writer = HarnessWriter(ioContext = EmptyCoroutineContext)

    private val moduleDirectory = createTempDirectory("kaleidos-test").toString()

    private fun sources(vararg files: GeneratedFile): List<GeneratedModuleSources> {
        return listOf(GeneratedModuleSources(moduleDirectory = moduleDirectory, files = files.toList()))
    }

    @Test
    fun GIVEN_planned_sources_WHEN_write_THEN_they_land_in_the_build_directory_of_the_module() = runTest {
        writer.write(sources(GeneratedFile("kotlin/com/example/PreviewRegistry.kt", "registry")), layout)

        val file = layout.root(moduleDirectory).resolve("kotlin/com/example/PreviewRegistry.kt")
        assertEquals("registry", file.readText())
    }

    @Test
    fun GIVEN_a_file_of_an_earlier_run_WHEN_it_is_no_longer_planned_THEN_it_is_deleted() = runTest {
        val stale = layout.root(moduleDirectory).resolve("kotlin/com/example/OldRegistry.kt")
        stale.createParentDirectories()
        stale.writeText("old")

        writer.write(sources(GeneratedFile("kotlin/com/example/PreviewRegistry.kt", "registry")), layout)

        assertFalse(stale.exists())
        assertTrue(layout.root(moduleDirectory).resolve("kotlin/com/example/PreviewRegistry.kt").exists())
    }

    /** A rewritten file would restart the continuous Gradle build on every switch between two files. */
    @Test
    fun GIVEN_unchanged_content_WHEN_write_again_THEN_the_file_is_not_touched() = runTest {
        val planned = sources(GeneratedFile("kotlin/com/example/PreviewRegistry.kt", "registry"))
        writer.write(planned, layout)
        val file = layout.root(moduleDirectory).resolve("kotlin/com/example/PreviewRegistry.kt")
        val writtenAt = FileTime.fromMillis(0)
        file.setLastModifiedTime(writtenAt)

        writer.write(planned, layout)

        assertEquals(writtenAt, file.getLastModifiedTime())
    }

    @Test
    fun GIVEN_changed_content_WHEN_write_again_THEN_the_file_is_replaced() = runTest {
        writer.write(sources(GeneratedFile("kotlin/com/example/PreviewRegistry.kt", "registry")), layout)

        writer.write(sources(GeneratedFile("kotlin/com/example/PreviewRegistry.kt", "changed")), layout)

        val file = layout.root(moduleDirectory).resolve("kotlin/com/example/PreviewRegistry.kt")
        assertEquals("changed", file.readText())
    }
}
