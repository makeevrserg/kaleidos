package com.makeevrserg.kaleidos.harness.generator

import com.makeevrserg.kaleidos.harness.HarnessLayout
import com.makeevrserg.kaleidos.harness.PreviewModulePlan

/**
 * The registry compiled into a module that owns previews. It is what makes `internal` previews work:
 * the calls happen inside their own module, only the object that dispatches them is public.
 */
class PreviewRegistryFactory(private val naming: HarnessNaming) {

    private fun fqnLines(module: PreviewModulePlan): List<String> {
        return module.previews.mapIndexed { index, preview ->
            val separator = if (index == module.previews.lastIndex) "" else ","
            "        \"${preview.fqn}\"$separator"
        }
    }

    private fun renderLines(module: PreviewModulePlan): List<String> {
        return module.previews.map { preview -> "            \"${preview.fqn}\" -> ${preview.fqn}()" }
    }

    fun create(module: PreviewModulePlan): GeneratedFile {
        val packageName = naming.registryPackage(module.gradlePath)
        val lines = buildList {
            add(HarnessNaming.HEADER)
            add("package $packageName")
            add("")
            add("import androidx.compose.runtime.Composable")
            add("")
            add("public object ${HarnessNaming.REGISTRY_CLASS} {")
            add("    public val fqns: List<String> = listOf(")
            addAll(fqnLines(module))
            add("    )")
            add("")
            add("    @Composable")
            add("    public fun Render(fqn: String) {")
            add("        when (fqn) {")
            addAll(renderLines(module))
            add("            else -> Unit")
            add("        }")
            add("    }")
            add("}")
        }
        return GeneratedFile(
            relativePath = "${HarnessLayout.KOTLIN_DIRECTORY}/${naming.packageDirectory(packageName)}/" +
                "${HarnessNaming.REGISTRY_CLASS}.kt",
            content = lines.joinToString(separator = "\n", postfix = "\n")
        )
    }
}
