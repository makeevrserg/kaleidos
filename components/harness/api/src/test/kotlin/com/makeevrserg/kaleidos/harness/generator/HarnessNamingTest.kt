package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessFixtures.preview
import com.makeevrserg.kaleidos.harness.HarnessFixtures.privatePreview
import com.makeevrserg.kaleidos.harness.HarnessPreview
import kotlin.test.Test
import kotlin.test.assertEquals

class HarnessNamingTest {
    private val naming = HarnessNaming()

    @Test
    fun GIVEN_gradle_path_with_dashes_WHEN_registry_package_THEN_it_is_a_valid_package() {
        assertEquals(
            "kaleidos.generated.m_components_lib_kobweb_ui",
            naming.registryPackage(":components:lib-kobweb:ui")
        )
    }

    @Test
    fun GIVEN_root_project_WHEN_registry_package_THEN_the_segment_is_still_an_identifier() {
        assertEquals("kaleidos.generated.m", naming.registryPackage(""))
    }

    @Test
    fun GIVEN_two_modules_WHEN_registry_fqn_THEN_the_registries_do_not_collide() {
        val first = naming.registryFqn(":components:ui")
        val second = naming.registryFqn(":instances:ui")

        assertEquals("kaleidos.generated.m_components_ui.PreviewRegistry", first)
        assertEquals("kaleidos.generated.m_instances_ui.PreviewRegistry", second)
    }

    @Test
    fun GIVEN_public_preview_WHEN_call_fqn_THEN_the_preview_is_called_directly() {
        assertEquals("com.example.CardPreview", naming.callFqn(preview("com.example.CardPreview")))
    }

    @Test
    fun GIVEN_private_preview_WHEN_call_fqn_THEN_its_wrapper_in_the_same_package_is_called() {
        assertEquals(
            "com.example.KaleidosPreview_com_example_CardPreview_CardPreview",
            naming.callFqn(privatePreview("com.example.CardPreview"))
        )
    }

    @Test
    fun GIVEN_private_preview_in_the_root_package_WHEN_call_fqn_THEN_the_wrapper_is_called_by_its_name() {
        val preview = HarnessPreview(fqn = "CardPreview", sourcePath = "CardPreview.kt", isPrivate = true)

        assertEquals("KaleidosPreview_CardPreview_CardPreview", naming.callFqn(preview))
    }

    @Test
    fun GIVEN_private_previews_of_one_name_in_two_files_WHEN_wrapper_name_THEN_they_do_not_collide() {
        val card = HarnessPreview(fqn = "com.example.Preview", sourcePath = "com/example/Card.kt", isPrivate = true)
        val button = card.copy(sourcePath = "com/example/Button.kt")

        assertEquals("KaleidosPreview_com_example_Card_Preview", naming.privatePreviewWrapperName(card))
        assertEquals("KaleidosPreview_com_example_Button_Preview", naming.privatePreviewWrapperName(button))
    }

    @Test
    fun GIVEN_file_name_that_is_not_an_identifier_WHEN_wrapper_name_THEN_it_is_one() {
        val preview = HarnessPreview(
            fqn = "com.example.Preview",
            sourcePath = "com/example/card-view.kt",
            isPrivate = true
        )

        assertEquals("KaleidosPreview_com_example_card_view_Preview", naming.privatePreviewWrapperName(preview))
    }

    @Test
    fun GIVEN_source_path_WHEN_copy_path_THEN_it_lies_under_the_directory_of_copies() {
        assertEquals(
            "kotlin/kaleidos/sources/com/example/Card.kt",
            naming.privatePreviewFileCopyPath("com/example/Card.kt")
        )
    }
}
