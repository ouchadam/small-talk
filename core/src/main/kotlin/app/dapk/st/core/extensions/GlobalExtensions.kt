package app.dapk.st.core.extensions

inline fun <T> T?.ifNull(block: () -> T): T = this ?: block()
inline fun <T> ifOrNull(condition: Boolean, block: () -> T): T? = if (condition) block() else null

@Suppress("UNCHECKED_CAST")
inline fun <T, T1 : T, T2 : T> Iterable<T>.firstOrNull(predicate: (T) -> Boolean, predicate2: (T) -> Boolean): Pair<T1, T2>? {
    var firstValue: T1? = null
    var secondValue: T2? = null

    for (element in this) {
        if (firstValue == null && predicate(element)) {
            firstValue = element as T1
        }
        if (secondValue == null && predicate2(element)) {
            secondValue = element as T2
        }
        if (firstValue != null && secondValue != null) return firstValue to secondValue
    }
    return null
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> = lazy(mode = LazyThreadSafetyMode.NONE, initializer = initializer)
