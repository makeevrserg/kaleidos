package com.makeevrserg.kaleidos.psi.model

import com.makeevrserg.kaleidos.harness.model.HarnessPreview
import com.makeevrserg.kaleidos.harness.model.PrivatePreviewFile

data class ScannedPreviewFile(
    val moduleDirectory: String,
    val previews: List<HarnessPreview>,
    val privatePreviewFile: PrivatePreviewFile?
)
