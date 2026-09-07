package com.makeevrserg.kaleidos.core

import kotlinx.coroutines.CoroutineScope

/**
 * Coroutine scope of a feature. Implementations are children of the owning service scope,
 * so they are cancelled together with the project.
 */
interface CoroutineFeature : CoroutineScope
