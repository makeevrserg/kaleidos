package com.makeevrserg.kaleidos.core.coroutine.model

import kotlin.coroutines.CoroutineContext

/**
 * Dispatchers of the host application. Plain modules never touch `Dispatchers` directly, so the
 * IDE can supply its event dispatch thread and tests can supply a test dispatcher.
 *
 * Typed as [CoroutineContext] because the IntelliJ EDT dispatcher carries a modality state as well.
 *
 * @property main UI thread; Swing components may only be touched here
 * @property io blocking I/O such as sockets and files
 * @property default CPU-bound background work and read actions
 */
data class PreviewDispatchers(
    val main: CoroutineContext,
    val io: CoroutineContext,
    val default: CoroutineContext
)
