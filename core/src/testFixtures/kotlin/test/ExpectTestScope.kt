package test

import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext

fun runExpectTest(testBody: suspend ExpectTestScope.() -> Unit) {
    runTest {
        val expectTest = ExpectTest(coroutineContext)
        testBody(expectTest)
    }

}

class ExpectTest(override val coroutineContext: CoroutineContext) : ExpectTestScope {

    private val expects = mutableListOf<Pair<Int, suspend MockKVerificationScope.() -> Unit>>()
    private val groups = mutableListOf<suspend MockKVerificationScope.() -> Unit>()

    override fun verifyExpects() {
        expects.forEach { (times, block) -> coVerify(exactly = times) { block.invoke(this) } }
        groups.forEach { coVerifyOrder { it.invoke(this) } }
    }

    override fun <T> T.expectUnit(times: Int, block: suspend MockKMatcherScope.(T) -> Unit) {
        coJustRun { block(this@expectUnit) }.ignore()
        expects.add(times to { block(this@expectUnit) })
    }

    override fun <T> T.expect(times: Int, block: suspend MockKMatcherScope.(T) -> Unit) {
        coJustRun { block(this@expect) }
        expects.add(times to { block(this@expect) })
    }

    override fun <T> T.captureExpects(block: suspend MockKMatcherScope.(T) -> Unit) {
        groups.add { block(this@captureExpects) }
    }
}

private fun Any.ignore() = Unit

interface ExpectTestScope : CoroutineScope {
    fun verifyExpects()
    fun <T> T.expectUnit(times: Int = 1, block: suspend MockKMatcherScope.(T) -> Unit)
    fun <T> T.expect(times: Int = 1, block: suspend MockKMatcherScope.(T) -> Unit)
    fun <T> T.captureExpects(block: suspend MockKMatcherScope.(T) -> Unit)
}