import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.amshove.kluent.internal.assertEquals
import test.ExpectTestScope
import test.FlowTestObserver

@Suppress("UNCHECKED_CAST")
internal class ViewModelTestScopeImpl(
    private val expectTestScope: ExpectTestScope,
    private val stateProvider: ViewModelTest
) : ExpectTestScope by expectTestScope, ViewModelTestScope {

    private val flowObserverScope = CoroutineScope(Dispatchers.Unconfined)

    private var viewEvents: FlowTestObserver<*>? = null
    private val viewStates = mutableListOf<Any>()
    private var capturedInitialState: Any? = null

    override fun <S, VE, T : DapkViewModel<S, VE>> T.test(initialState: S?): T {
        initialState?.let { stateProvider.instance?.value = it }
        capturedInitialState = stateProvider.instance?.value
        stateProvider.instance?.onValue = { viewStates.add(it) }
        viewEvents = FlowTestObserver(flowObserverScope, this.events)
        return this
    }

    override fun <S> assertStates(states: List<S>) {
        assertEquals(states, viewStates)
    }

    override fun <VE> assertEvents(events: List<VE>) {
        (viewEvents as? FlowTestObserver<VE>)?.assertValues(events)
    }

    override fun <S> assertInitialState(state: S) {
        assertEquals(state, capturedInitialState)
    }

    override fun <S> assertStates(vararg state: (S) -> S) {
        val states = state.toList().map { it as (Any) -> Any }.fold(mutableListOf<Any>()) { acc, curr ->
            curr.invoke(acc.lastOrNull() ?: capturedInitialState!!).let { acc.add(it) }; acc
        }
        assertStates(states)
    }

    fun finish() {
        viewStates.clear()
        viewEvents?.finish()
    }
}

interface ViewModelTestScope : ExpectTestScope {
    fun <S, VE, T : DapkViewModel<S, VE>> T.test(initialState: S? = null): T

    fun <VE> assertEvents(vararg event: VE) = this.assertEvents(event.toList())
    fun <VE> assertNoEvents() = this.assertEvents<VE>(emptyList())
    fun <VE> assertEvents(events: List<VE>)

    fun <S> assertInitialState(state: S)
    fun <S> assertStates(vararg state: S.() -> S)
    fun <S> assertStates(vararg state: S) = this.assertStates(state.toList())
    fun <S> assertNoStates() = this.assertStates<S>(emptyList())
    fun <S> assertStates(states: List<S>)
}

