package app.dapk.st.core.extensions

import app.dapk.st.core.Lce

fun <T> Lce<T>.takeIfContent(): T? {
    return when (this) {
        is Lce.Content -> this.value
        is Lce.Error -> null
        is Lce.Loading -> null
    }
}