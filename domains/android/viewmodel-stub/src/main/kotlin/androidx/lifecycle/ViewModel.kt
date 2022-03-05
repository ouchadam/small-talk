package androidx.lifecycle

abstract class ViewModel {

    protected open fun onCleared() {}

    fun clear() {
        throw RuntimeException("stub")
    }

    fun <T> setTagIfAbsent(key: String, newValue: T): T {
        throw RuntimeException("stub")
    }

    fun <T> getTag(key: String): T? {
        throw RuntimeException("stub")
    }

}
