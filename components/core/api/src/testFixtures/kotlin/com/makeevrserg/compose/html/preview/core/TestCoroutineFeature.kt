package com.makeevrserg.compose.html.preview.core

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/** Runs the feature inside the scope of a `runTest` block so virtual time applies. */
class TestCoroutineFeature(scope: CoroutineScope) : CoroutineFeature {
    override val coroutineContext: CoroutineContext = scope.coroutineContext
}
