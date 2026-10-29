package com.makeevrserg.kaleidos.feature.fake

import com.makeevrserg.kaleidos.feature.api.PreviewToolWindowPresenter

class FakePreviewToolWindowPresenter : PreviewToolWindowPresenter {
    var showCount = 0

    override fun show() {
        showCount++
    }
}
