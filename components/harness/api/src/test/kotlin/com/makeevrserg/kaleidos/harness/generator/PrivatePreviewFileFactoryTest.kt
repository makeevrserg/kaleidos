package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessFixtures.preview
import com.makeevrserg.kaleidos.harness.HarnessFixtures.privatePreview
import com.makeevrserg.kaleidos.harness.HarnessPreview
import com.makeevrserg.kaleidos.harness.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.PrivatePreviewFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivatePreviewFileFactoryTest {
    private val factory = PrivatePreviewFileFactory(HarnessNaming())

    private val cardFile = PrivatePreviewFile(
        sourcePath = "com/example/Card.kt",
        text = """
            |package com.example
            |
            |@Preview
            |@Composable
            |private fun CardPreview() {}
            |
            |
        """.trimMargin()
    )

    private fun inCardFile(preview: HarnessPreview): HarnessPreview = preview.copy(sourcePath = cardFile.sourcePath)

    private val module = PreviewModulePlan(
        gradlePath = ":components:ui",
        directory = "/project/components/ui",
        previews = listOf(
            inCardFile(privatePreview("com.example.CardPreview")),
            inCardFile(preview("com.example.CardPublicPreview")),
            privatePreview("com.example.ButtonPreview")
        ),
        privatePreviewFiles = listOf(cardFile)
    )

    @Test
    fun GIVEN_file_with_a_private_preview_WHEN_create_THEN_it_is_copied_under_the_directory_of_copies() {
        val copy = factory.create(module).single()

        assertEquals("kotlin/kaleidos/sources/com/example/Card.kt", copy.relativePath)
        assertTrue(copy.content.startsWith(HarnessNaming.HEADER), copy.content)
    }

    @Suppress("StringShouldBeRawString")
    @Test
    fun GIVEN_file_with_a_private_preview_WHEN_create_THEN_the_copy_keeps_the_source_and_adds_a_wrapper() {
        val content = factory.create(module).single().content

        assertTrue(content.contains(cardFile.text.trimEnd()), content)
        assertTrue(
            content.endsWith(
                "private fun CardPreview() {}\n" +
                    "\n" +
                    "@androidx.compose.runtime.Composable\n" +
                    "internal fun KaleidosPreview_com_example_Card_CardPreview() {\n" +
                    "    CardPreview()\n" +
                    "}\n"
            ),
            content
        )
    }

    @Test
    fun GIVEN_public_preview_in_the_same_file_WHEN_create_THEN_it_gets_no_wrapper() {
        val content = factory.create(module).single().content

        assertFalse(content.contains("CardPublicPreview"), content)
    }

    @Test
    fun GIVEN_private_preview_of_another_file_WHEN_create_THEN_it_is_not_wrapped_in_this_copy() {
        val content = factory.create(module).single().content

        assertFalse(content.contains("ButtonPreview"), content)
    }

    @Test
    fun GIVEN_module_without_private_previews_WHEN_create_THEN_nothing_is_copied() {
        val files = factory.create(module.copy(privatePreviewFiles = emptyList()))

        assertTrue(files.isEmpty())
    }
}
