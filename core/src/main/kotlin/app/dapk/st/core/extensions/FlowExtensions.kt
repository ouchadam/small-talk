package app.dapk.st.core.extensions

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile

@OptIn(InternalCoroutinesApi::class)
suspend fun <T> Flow<T>.firstOrNull(count: Int, predicate: suspend (T) -> Boolean): T? {
    var counter = 0

    var result: T? = null
    this
        .takeWhile {
            counter++
            !predicate(it) || counter < (count + 1)
        }
        .filter { predicate(it) }
        .collect {
            result = it
        }

    return result
}