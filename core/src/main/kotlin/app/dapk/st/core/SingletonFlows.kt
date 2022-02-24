package app.dapk.st.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class SingletonFlows {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, MutableSharedFlow<*>>()
    private val started = ConcurrentHashMap<String, Boolean?>()

    @Suppress("unchecked_cast")
    suspend fun <T> getOrPut(key: String, onStart: suspend () -> T): Flow<T> {
        return when (val flow = cache[key]) {
            null -> mutex.withLock {
                cache.getOrPut(key) {
                    MutableSharedFlow<T>(replay = 1).also {
                        withContext(Dispatchers.IO) {
                            async {
                                it.emit(onStart())
                            }
                        }
                    }
                } as Flow<T>
            }
            else -> flow as Flow<T>
        }
    }

    fun <T> get(key: String): Flow<T> {
        return cache[key]!! as Flow<T>
    }

    suspend fun <T> update(key: String, value: T) {
        (cache[key] as? MutableSharedFlow<T>)?.emit(value)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

}