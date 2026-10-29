package com.makeevrserg.kaleidos.harness.api

import com.makeevrserg.kaleidos.harness.generator.model.GeneratedModuleSources

interface HarnessWriter {
    suspend fun write(sources: List<GeneratedModuleSources>)
}
