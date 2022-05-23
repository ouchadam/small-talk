package app.dapk.st.core.extensions

fun <K, V> Map<K, V>?.containsKey(key: K) = this?.containsKey(key) ?: false

fun <K, V> MutableMap<K,V>.clearAndPutAll(input: Map<K, V>) {
    this.clear()
    this.putAll(input)
}