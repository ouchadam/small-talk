package test

import io.mockk.MockKMatcherScope
import io.mockk.MockKVerificationScope
import io.mockk.coJustRun
import io.mockk.coVerifyAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest

fun runExpectTest(testBody: suspend ExpectTestScope.() -> Unit) {
    val expects = mutableListOf<suspend MockKVerificationScope.() -> Unit>()
    runTest {
        testBody(object : ExpectTestScope {
            override val coroutineContext = this@runTest.coroutineContext
            override fun verifyExpects() = coVerifyAll { expects.forEach { it.invoke(this@coVerifyAll) } }
            override fun <T> T.expectUnit(block: suspend MockKMatcherScope.(T) -> Unit) {
                coJustRun { block(this@expectUnit) }.ignore()
                expects.add { block(this@expectUnit) }
            }
        })
    }
}

private fun Any.ignore() = Unit

interface ExpectTestScope : CoroutineScope {
    fun verifyExpects()
    fun <T> T.expectUnit(block: suspend MockKMatcherScope.(T) -> Unit)
}