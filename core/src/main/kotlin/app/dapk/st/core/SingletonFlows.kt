package app.dapk.st.core

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SingletonFlows(
    private val coroutineDispatchers: CoroutineDispatchers
) {

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, MutableSharedFlow<*>>()

    @Suppress("unchecked_cast")
    suspend fun <T> getOrPut(key: String, onStart: suspend () -> T): Flow<T> {
        return when (val flow = cache[key]) {
            null -> mutex.withLock {
                cache.getOrPut(key) {
                    MutableSharedFlow<T>(replay = 1).also {
                        coroutineDispatchers.withIoContext {
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

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): Flow<T> {
        return cache[key]!! as Flow<T>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> update(key: String, value: T) {
        (cache[key] as? MutableSharedFlow<T>)?.emit(value)
    }
}