package app.dapk.st.core

sealed interface Lce<T> {
    class Loading<T> : Lce<T>
    data class Error<T>(val cause: Throwable) : Lce<T>
    data class Content<T>(val value: T) : Lce<T>
}

