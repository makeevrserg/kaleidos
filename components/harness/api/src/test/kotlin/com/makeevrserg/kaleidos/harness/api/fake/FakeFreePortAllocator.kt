package com.makeevrserg.kaleidos.harness.api.fake

import com.makeevrserg.kaleidos.harness.api.api.FreePortAllocator

class FakeFreePortAllocator(private val port: Int) : FreePortAllocator {
    val keys = mutableListOf<String>()

    override fun allocate(key: String): Int {
        keys.add(key)
        return port
    }
}
