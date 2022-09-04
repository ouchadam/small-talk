package app.dapk.st.core

sealed interface Lce<T> {
    data class Loading<T>(val ignored: Unit = Unit) : Lce<T>
    data class Error<T>(val cause: Throwable) : Lce<T>
    data class Content<T>(val value: T) : Lce<T>
}

sealed interface LceWithProgress<T> {
    data class Loading<T>(val progress: T) : LceWithProgress<T>
    data class Error<T>(val cause: Throwable) : LceWithProgress<T>
    data class Content<T>(val value: T) : LceWithProgress<T>
}

