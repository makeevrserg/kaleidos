package com.makeevrserg.kaleidos.harness.fake

import com.makeevrserg.kaleidos.harness.api.ProjectPreviewSource
import com.makeevrserg.kaleidos.harness.model.ModulePreviews

class FakeProjectPreviewSource(
    private val previews: List<ModulePreviews>,
    private val entryPoints: Map<String, List<String>> = emptyMap()
) : ProjectPreviewSource {

    override suspend fun previews(): List<ModulePreviews> = previews

    override suspend fun entryPointSourcePaths(moduleDirectory: String): List<String> {
        return entryPoints[moduleDirectory].orEmpty()
    }
}
