package com.makeevrserg.compose.html.preview.harness

import com.makeevrserg.compose.html.preview.harness.HarnessFixtures.directoryOf
import com.makeevrserg.compose.html.preview.harness.HarnessFixtures.host
import com.makeevrserg.compose.html.preview.harness.HarnessFixtures.previews
import com.makeevrserg.compose.html.preview.harness.HarnessFixtures.structure
import com.makeevrserg.compose.html.preview.harness.HarnessFixtures.structureOf
import com.makeevrserg.compose.html.preview.host.DevServerKind
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HarnessPlannerTest {
    private val webpackHost = host(":components:ui", DevServerKind.WEBPACK)
    private val kobwebHost = host(":instances:web-app", DevServerKind.KOBWEB)

    private fun planner(
        source: ProjectPreviewSource,
        allocator: FreePortAllocator = FakeFreePortAllocator(PORT)
    ) = HarnessPlanner(projectPreviewSource = source, portAllocator = allocator)

    @Test
    fun GIVEN_previews_of_a_module_the_host_does_not_depend_on_WHEN_plan_THEN_they_are_left_out() = runTest {
        val source = FakeProjectPreviewSource(
            listOf(
                previews(":components:ui", "com.example.CardPreview"),
                previews(":components:other", "com.other.OtherPreview")
            )
        )

        val plan = planner(source).plan(webpackHost, structureOf(":components:ui", ":components:other"))

        assertEquals(listOf(":components:ui"), plan.modules.map { module -> module.gradlePath })
    }

    @Test
    fun GIVEN_previews_in_a_dependency_of_the_host_WHEN_plan_THEN_a_registry_is_planned_for_it() = runTest {
        val source = FakeProjectPreviewSource(listOf(previews(":components:ui", "com.example.CardPreview")))

        val plan = planner(source).plan(kobwebHost, structure(":instances:web-app" to ":components:ui"))

        assertEquals(listOf(":components:ui"), plan.modules.map { module -> module.gradlePath })
        assertEquals(directoryOf(":components:ui"), plan.modules.single().directory)
    }

    @Test
    fun GIVEN_modules_and_previews_in_any_order_WHEN_plan_THEN_the_result_is_stable() = runTest {
        val source = FakeProjectPreviewSource(
            listOf(
                previews(":components:z", "com.example.ZPreview"),
                previews(":components:a", "com.example.SecondPreview", "com.example.FirstPreview")
            )
        )
        val structure = structure(":instances:web-app" to ":components:z", ":instances:web-app" to ":components:a")

        val plan = planner(source).plan(kobwebHost, structure)

        assertEquals(listOf(":components:a", ":components:z"), plan.modules.map { module -> module.gradlePath })
        assertEquals(
            listOf("com.example.FirstPreview", "com.example.SecondPreview"),
            plan.modules.first().previews.map { preview -> preview.fqn }
        )
    }

    @Test
    fun GIVEN_a_module_without_previews_WHEN_plan_THEN_no_registry_is_generated_into_it() = runTest {
        val source = FakeProjectPreviewSource(listOf(previews(":components:ui")))

        val plan = planner(source).plan(webpackHost, structureOf(":components:ui"))

        assertTrue(plan.modules.isEmpty())
    }

    @Test
    fun GIVEN_webpack_host_WHEN_plan_THEN_it_gets_a_port_and_loses_its_own_entry_points() = runTest {
        val source = FakeProjectPreviewSource(
            previews = listOf(previews(":components:ui", "com.example.CardPreview")),
            entryPoints = mapOf(directoryOf(":components:ui") to listOf("com/example/Main.kt"))
        )
        val allocator = FakeFreePortAllocator(PORT)

        val plan = planner(source, allocator).plan(webpackHost, structureOf(":components:ui"))

        assertEquals(PORT, plan.host.devServerPort)
        assertEquals(listOf("com/example/Main.kt"), plan.host.excludedSourcePaths)
        assertEquals(listOf(directoryOf(":components:ui")), allocator.keys)
    }

    @Test
    fun GIVEN_kobweb_host_WHEN_plan_THEN_the_port_stays_with_the_kobweb_configuration() = runTest {
        val source = FakeProjectPreviewSource(
            previews = listOf(previews(":instances:web-app", "com.example.CardPreview")),
            entryPoints = mapOf(directoryOf(":instances:web-app") to listOf("com/example/Main.kt"))
        )

        val plan = planner(source).plan(kobwebHost, structureOf(":instances:web-app"))

        assertNull(plan.host.devServerPort)
        assertTrue(plan.host.excludedSourcePaths.isEmpty())
    }

    private companion object {
        const val PORT = 8301
    }
}
