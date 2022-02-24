package app.dapk.st.core.extensions

interface ErrorTracker {
    fun track(throwable: Throwable, extra: String = "")
}

interface CrashScope {
    val errorTracker: ErrorTracker
    fun <T> Result<T>.trackFailure() = this.onFailure { errorTracker.track(it) }
}

fun <T> ErrorTracker.nullAndTrack(throwable: Throwable, extra: String = ""): T? {
    this.track(throwable, extra)
    return null
}