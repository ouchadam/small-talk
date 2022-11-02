package test

import app.dapk.state.Action
import app.dapk.state.Reducer
import app.dapk.state.ReducerFactory
import app.dapk.state.ReducerScope
import fake.FakeEventSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo

interface ReducerTest<S, E> {
    operator fun invoke(block: suspend ReducerTestScope<S, E>.() -> Unit)
}

fun <S, E> testReducer(block: ((E) -> Unit) -> ReducerFactory<S>): ReducerTest<S, E> {
    val fakeEventSource = FakeEventSource<E>()
    val reducerFactory = block(fakeEventSource)
    return object : ReducerTest<S, E> {
        override fun invoke(block: suspend ReducerTestScope<S, E>.() -> Unit) {
            runReducerTest(reducerFactory, fakeEventSource, block)
        }
    }
}

fun <S, E> runReducerTest(reducerFactory: ReducerFactory<S>, fakeEventSource: FakeEventSource<E>, block: suspend ReducerTestScope<S, E>.() -> Unit) {
    runTest {
        val expectTestScope = ExpectTest(coroutineContext)
        block(ReducerTestScope(reducerFactory, fakeEventSource, expectTestScope))
        expectTestScope.verifyExpects()
    }
}

class ReducerTestScope<S, E>(
    private val reducerFactory: ReducerFactory<S>,
    private val fakeEventSource: FakeEventSource<E>,
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

    override fun reduce(action: Action) = reducer.reduce(action).also {
        capturedResult = it
    }

    fun setState(state: S) {
        manualState = state
    }

    fun setState(block: (S) -> S) {
        manualState = block(reducerScope.getState())
    }

    fun assertInitialState(expected: S) {
        reducerFactory.initialState() shouldBeEqualTo expected
    }

    fun assertEvents(events: List<E>) {
        fakeEventSource.assertEvents(events)
    }

    fun assertOnlyStateChange(expected: S) {
        assertStateChange(expected)
        assertNoDispatches()
        fakeEventSource.assertNoEvents()
    }

    fun assertOnlyStateChange(block: (S) -> S) {
        val expected = block(reducerScope.getState())
        assertStateChange(expected)
        assertNoDispatches()
        fakeEventSource.assertNoEvents()
    }

    fun assertStateChange(expected: S) {
        capturedResult shouldBeEqualTo expected
    }

    fun assertDispatches(expected: List<Action>) {
        assertEquals(expected, actionCaptures)
    }

    fun assertNoDispatches() {
        assertEquals(emptyList(), actionCaptures)
    }

    fun assertNoStateChange() {
        assertEquals(reducerScope.getState(), capturedResult)
    }

    fun assertNoEvents() {
        fakeEventSource.assertNoEvents()
    }

    fun assertOnlyDispatches(expected: List<Action>) {
        assertDispatches(expected)
        fakeEventSource.assertNoEvents()
        assertNoStateChange()
    }

    fun assertOnlyEvents(events: List<E>) {
        fakeEventSource.assertEvents(events)
        assertNoDispatches()
        assertNoStateChange()
    }

    fun assertNoChanges() {
        assertNoStateChange()
        assertNoEvents()
        assertNoDispatches()
    }
}