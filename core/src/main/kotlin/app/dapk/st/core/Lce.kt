package app.dapk.st.core

sealed interface Lce<T> {
    data class Loading<T>(val ignored: Unit = Unit) : Lce<T>
    data class Error<T>(val cause: Throwable) : Lce<T>
    data class Content<T>(val value: T) : Lce<T>
}

