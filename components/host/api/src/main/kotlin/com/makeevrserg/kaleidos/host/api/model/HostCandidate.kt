package com.makeevrserg.kaleidos.host.api.model

/** A module that has a dev server task, paired with the kind of server that task starts. */
data class HostCandidate(
    val module: GradleProject,
    val kind: DevServerKind
)
