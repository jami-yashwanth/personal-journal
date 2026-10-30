package com.personal.journal.ui.motion

import coil3.memory.MemoryCache

/** Where each Day tile's decoded photo sits in Coil's memory cache, so the Entry hero can show it while the full size loads. */
class ImageKeys {
    private val keys = object : LinkedHashMap<String, MemoryCache.Key>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoryCache.Key>) = size > MAX
    }

    fun put(entryId: String, key: MemoryCache.Key) = synchronized(keys) { keys[entryId] = key }

    fun get(entryId: String): MemoryCache.Key? = synchronized(keys) { keys[entryId] }

    private companion object {
        const val MAX = 200
    }
}
