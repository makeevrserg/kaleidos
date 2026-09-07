package com.makeevrserg.kaleidos.harness.generator

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
}
