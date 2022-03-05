package test

import io.mockk.*

inline fun <T : Any, reified R> T.expect(crossinline block: suspend MockKMatcherScope.(T) -> R) {
    coEvery { block(this@expect) } returns mockk(relaxed = true)
}

fun <T, B> MockKStubScope<T, B>.delegateReturn() = object : Returns<T> {
    override fun returns(value: T) {
        answers(ConstantAnswer(value))
    }

    override fun throws(value: Throwable) {
        this@delegateReturn.throws(value)
    }
}

fun <T> returns(block: (T) -> Unit) = object : Returns<T> {
    override fun returns(value: T) = block(value)
    override fun throws(value: Throwable) = throw value
}

interface Returns<T> {
    fun returns(value: T)
    fun throws(value: Throwable)
}
