package app.dapk.st.directory

import app.dapk.state.Action
import app.dapk.state.Reducer
import app.dapk.state.ReducerFactory
import app.dapk.state.ReducerScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import test.ExpectTest
import test.ExpectTestScope

fun <S> runReducerTest(reducerFactory: ReducerFactory<S>, block: suspend ReducerTestScope<S>.() -> Unit) {
    runTest {
        val expectTestScope = ExpectTest(coroutineContext)
        block(ReducerTestScope(reducerFactory, expectTestScope))
        expectTestScope.verifyExpects()
    }
}

class ReducerTestScope<S>(
    private val reducerFactory: ReducerFactory<S>,
    private val expectTestScope: ExpectTestScope
) : ExpectTestScope by expectTestScope, Reducer<S> {

    private var manualState: S? = null
    private var capturedResult: S? = null

    private val actionCaptures = mutableListOf<Action>()
    private val reducerScope = object : ReducerScope<S> {
        override val coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        override suspend fun dispatch(action: Action) {
            actionCaptures.add(action)
        }

        override fun getState() = manualState ?: reducerFactory.initialState()
    }
    private val reducer: Reducer<S> = reducerFactory.create(reducerScope)

    override suspend fun reduce(action: Action) = reducer.reduce(action).also {
        capturedResult = it
    }

    fun setState(state: S) {
        manualState = state
    }

    fun assertInitialState(expected: S) {
        reducerFactory.initialState() shouldBeEqualTo expected
    }

    fun <S> assertDispatches(expected: List<S>) {
        assertEquals(expected, actionCaptures)
    }

    fun assertNoDispatches() {
        assertEquals(emptyList(), actionCaptures)
    }

    fun assertNoStateChange() {
        assertEquals(reducerFactory.initialState(), capturedResult)
    }
}