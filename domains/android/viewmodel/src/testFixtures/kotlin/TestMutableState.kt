import androidx.compose.runtime.MutableState

class TestMutableState<T>(initialState: T) : MutableState<T> {

    private var _value: T = initialState

    var onValue: ((T) -> Unit)? = null

    override var value: T
        get() = _value
        set(value) {
            _value = value
            onValue?.invoke(value)
        }

    override fun component1(): T = throw RuntimeException("stub")
    override fun component2(): (T) -> Unit = throw RuntimeException("stub")
}