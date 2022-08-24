package app.dapk.st.core

class LRUCache<K, V>(val maxSize: Int) {

    private val internalCache = object : LinkedHashMap<K, V>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    fun put(key: K, value: V) {
        internalCache[key] = value
    }

    fun get(key: K): V? {
        return internalCache[key]
    }

    fun getOrPut(key: K, value: () -> V): V {
        return get(key) ?: value().also { put(key, it) }
    }

    fun size() = internalCache.size

}

fun LRUCache<*, *>?.isNullOrEmpty() = this == null || this.size() == 0