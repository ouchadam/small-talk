package app.dapk.st.messenger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test

class ImplBarTest {

    @Test
    fun `test bar`() {
        val viewModelScope = CoroutineScope(UnconfinedTestDispatcher())
        val reducer = FooBar.createReducer(
            initialState = ImplBar.BankState(amount = 0),
            FooBar.sideEffect(ImplBar.BankAction::class) { action, state ->
                println("SE ${action::class.simpleName} $state")
            },
            FooBar.sideEffect(ImplBar.BankAction.Foo::class) { action, state ->
                println("FOO - $action $state")
            },
            FooBar.change(ImplBar.BankAction.Increment::class) { _, state ->
                state.copy(amount = state.amount + 1)
            },
            FooBar.async(ImplBar.BankAction.MultiInc::class) { _ ->
                flowOf(0, 1, 2)
                    .onEach { dispatch(ImplBar.BankAction.Increment) }
                    .launchIn(coroutineScope)
            },
        )

        val store = FooBar.createStore(reducer, viewModelScope)
        store.subscribe {
            println(it)
        }
        runBlocking { store.dispatch(ImplBar.BankAction.Inc) }
    }

}