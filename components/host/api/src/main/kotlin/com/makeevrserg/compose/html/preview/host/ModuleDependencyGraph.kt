package com.makeevrserg.compose.html.preview.host

/**
 * Dependencies between Gradle module directories, folded from the IDE modules: a multiplatform module
 * is imported as several IDE modules (`jsMain`, `commonMain`, the holder) that share one directory.
 *
 * @param dependencies direct dependencies of each module directory, without self references
 */
data class ModuleDependencyGraph(val dependencies: Map<String, Set<String>>) {

    /** Transitive reachability, so a preview module sees libraries its direct dependencies pull in. */
    fun dependsOn(dependent: String, dependency: String): Boolean {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque(listOf(dependent))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            val direct = dependencies[current].orEmpty()
            if (dependency in direct) return true
            queue.addAll(direct)
        }
        return false
    }
}
