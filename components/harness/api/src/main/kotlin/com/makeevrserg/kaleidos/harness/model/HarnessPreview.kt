package com.makeevrserg.kaleidos.harness.model

/** @param fqn fully qualified name of a `@Preview` function the generated page can call */
data class HarnessPreview(
    val fqn: String,
    val sourcePath: String,
    val isPrivate: Boolean
)
