package com.makeevrserg.kaleidos.harness.generator.util

import com.makeevrserg.kaleidos.harness.generator.model.GeneratedFile
import com.makeevrserg.kaleidos.harness.model.PreviewModulePlan
import com.makeevrserg.kaleidos.harness.util.KOTLIN_DIRECTORY

private fun PreviewModulePlan.fqnLines(): List<String> {
    return previews.mapIndexed { index, preview ->
        val separator = if (index == previews.lastIndex) "" else ","
        "        \"${preview.fqn}\"$separator"
    }
}

private fun PreviewModulePlan.renderLines(): List<String> {
    return previews.map { preview ->
        "            \"${preview.fqn}\" -> ${preview.callFqn()}()"
    }
}

internal fun PreviewModulePlan.generatePreviewRegistry(): GeneratedFile {
    val packageName = registryPackage(gradlePath)
    val lines = buildList {
        add(HEADER)
        add("package $packageName")
        add("")
        add("import androidx.compose.runtime.Composable")
        add("")
        add("public object $REGISTRY_CLASS {")
        add("    public val fqns: List<String> = listOf(")
        addAll(fqnLines())
        add("    )")
        add("")
        add("    @Composable")
        add("    public fun Render(fqn: String) {")
        add("        when (fqn) {")
        addAll(renderLines())
        add("            else -> Unit")
        add("        }")
        add("    }")
        add("}")
    }
    return GeneratedFile(
        relativePath = "$KOTLIN_DIRECTORY/${packageDirectory(packageName)}/$REGISTRY_CLASS.kt",
        content = lines.joinToString(separator = "\n", postfix = "\n")
    )
}
