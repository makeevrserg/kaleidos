package com.makeevrserg.compose.html.preview.host

/** A module that has a dev server task, paired with the kind of server that task starts. */
data class HostCandidate(
    val module: GradleModule,
    val kind: DevServerKind
)
