package com.makeevrserg.kaleidos.host.util

import com.makeevrserg.kaleidos.host.model.PreviewProjectStructure.ModuleDependencyGraph

/** Transitive reachability, so a preview module sees libraries its direct dependencies pull in. */
fun ModuleDependencyGraph.dependsOn(dependent: String, dependency: String): Boolean {
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
