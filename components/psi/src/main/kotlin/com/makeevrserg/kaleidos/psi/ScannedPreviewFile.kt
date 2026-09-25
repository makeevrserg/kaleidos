package com.makeevrserg.kaleidos.psi

import com.makeevrserg.kaleidos.harness.HarnessPreview
import com.makeevrserg.kaleidos.harness.PrivatePreviewFile

data class ScannedPreviewFile(
    val moduleDirectory: String,
    val previews: List<HarnessPreview>,
    val privatePreviewFile: PrivatePreviewFile?
)
