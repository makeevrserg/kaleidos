package com.makeevrserg.kaleidos.core

import kotlin.coroutines.CoroutineContext

/**
 * Dispatchers of the host application. Plain modules never touch `Dispatchers` directly, so the
 * IDE can supply its event dispatch thread and tests can supply a test dispatcher.
 *
 * Typed as [CoroutineContext] because the IntelliJ EDT dispatcher carries a modality state as well.
 */
interface PreviewDispatchers {
    /** UI thread; Swing components may only be touched here. */
    val main: CoroutineContext

    /** Blocking I/O such as sockets and files. */
    val io: CoroutineContext

    /** CPU-bound background work and read actions. */
    val default: CoroutineContext
}
