package com.makeevrserg.compose.html.preview.harness

/**
 * What the plugin generates for one preview run: a registry in every module that owns previews and the
 * page in the module that serves them.
 *
 * @param modules ordered by Gradle path so the generated sources do not change when the IDE reports the
 * modules in another order
 */
data class HarnessPlan(
    val host: HarnessHostPlan,
    val modules: List<PreviewModulePlan>
)
