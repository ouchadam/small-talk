package app.dapk.st.core.extensions

import kotlinx.coroutines.flow.*

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

fun <T> Flow<T>.startAndIgnoreEmissions(): Flow<Boolean> = this.map { false }.onStart { emit(true) }.filter { it }