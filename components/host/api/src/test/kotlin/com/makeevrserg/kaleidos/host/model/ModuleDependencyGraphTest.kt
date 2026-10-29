@file:Suppress("FunctionNaming")

package com.makeevrserg.kaleidos.host.model

import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure.ModuleDependencyGraph
import com.makeevrserg.kaleidos.host.util.dependsOn
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModuleDependencyGraphTest {

    @Test
    fun GIVEN_direct_dependency_WHEN_depends_on_THEN_true() {
        val graph = ModuleDependencyGraph(mapOf("/app" to setOf("/lib")))

        assertTrue(graph.dependsOn(dependent = "/app", dependency = "/lib"))
    }

    @Test
    fun GIVEN_transitive_dependency_WHEN_depends_on_THEN_true() {
        val graph = ModuleDependencyGraph(
            mapOf(
                "/app" to setOf("/feature"),
                "/feature" to setOf("/lib")
            )
        )

        assertTrue(graph.dependsOn(dependent = "/app", dependency = "/lib"))
    }

    @Test
    fun GIVEN_reverse_direction_WHEN_depends_on_THEN_false() {
        val graph = ModuleDependencyGraph(mapOf("/app" to setOf("/lib")))

        assertFalse(graph.dependsOn(dependent = "/lib", dependency = "/app"))
    }

    @Test
    fun GIVEN_unknown_dependent_WHEN_depends_on_THEN_false() {
        val graph = ModuleDependencyGraph(mapOf("/app" to setOf("/lib")))

        assertFalse(graph.dependsOn(dependent = "/other", dependency = "/lib"))
    }

    @Test
    fun GIVEN_module_WHEN_depends_on_itself_THEN_false() {
        val graph = ModuleDependencyGraph(mapOf("/app" to setOf("/lib")))

        assertFalse(graph.dependsOn(dependent = "/app", dependency = "/app"))
    }

    @Test
    fun GIVEN_cycle_WHEN_dependency_is_not_reachable_THEN_terminates_with_false() {
        val graph = ModuleDependencyGraph(
            mapOf(
                "/a" to setOf("/b"),
                "/b" to setOf("/a")
            )
        )

        assertFalse(graph.dependsOn(dependent = "/a", dependency = "/c"))
    }
}
