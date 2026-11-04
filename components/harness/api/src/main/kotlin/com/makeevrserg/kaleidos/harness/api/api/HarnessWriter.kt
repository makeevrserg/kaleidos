package com.makeevrserg.kaleidos.harness.api.api

import com.makeevrserg.kaleidos.harness.api.generator.model.GeneratedModuleSources

interface HarnessWriter {
    suspend fun write(sources: List<GeneratedModuleSources>)
}
