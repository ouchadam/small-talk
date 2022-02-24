package test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take

fun Any.print() = this.also { println(this) }

suspend fun <T> Flow<T>.collectItem(count: Int): T {
    return this.take(count).last()
}

fun <T> T.unit() = Unit

