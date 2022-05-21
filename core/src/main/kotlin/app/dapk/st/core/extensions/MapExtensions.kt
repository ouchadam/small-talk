package app.dapk.st.core.extensions

fun <K, V> Map<K, V>?.containsKey(key: K) = this?.containsKey(key) ?: false