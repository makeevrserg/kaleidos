package com.makeevrserg.kaleidos.preview.api.feature.fake

import com.makeevrserg.kaleidos.preview.api.feature.api.PreviewToolWindowPresenter

class FakePreviewToolWindowPresenter : PreviewToolWindowPresenter {
    var showCount = 0

    override fun show() {
        showCount++
    }
}
