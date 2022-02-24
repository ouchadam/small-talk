package test

import io.mockk.*

inline fun <T : Any, reified R> T.expect(crossinline block: suspend MockKMatcherScope.(T) -> R) {
    coEvery { block(this@expect) } returns mockk(relaxed = true)
}

fun <T, B> MockKStubScope<T, B>.delegateReturn(): Returns<T> = Returns { value ->
    answers(ConstantAnswer(value))
}

fun interface Returns<T> {
    fun returns(value: T)
}