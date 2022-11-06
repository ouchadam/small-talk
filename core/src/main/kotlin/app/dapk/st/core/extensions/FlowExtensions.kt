package app.dapk.st.core.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile

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

inline fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> {
    return kotlinx.coroutines.flow.combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
        )
    }
}

