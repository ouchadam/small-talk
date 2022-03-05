@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import kotlin.reflect.KProperty

interface State<out T> {
    val value: T
}

interface MutableState<T> : State<T> {
    override var value: T
    operator fun component1(): T
    operator fun component2(): (T) -> Unit
}

operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = throw RuntimeException("stub")
operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    throw RuntimeException("stub")
}

fun <T> mutableStateOf(value: T): MutableState<T> = throw RuntimeException("stub")
