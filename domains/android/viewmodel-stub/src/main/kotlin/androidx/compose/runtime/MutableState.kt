@file:JvmName("SnapshotStateKt")

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

fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = object : SnapshotMutationPolicy<T> {
        override fun equivalent(a: T, b: T): Boolean {
            throw RuntimeException("stub")
        }
    }
): MutableState<T> = throw RuntimeException("stub")

interface SnapshotMutationPolicy<T> {
    fun equivalent(a: T, b: T): Boolean
    fun merge(previous: T, current: T, applied: T): T? = null
}
