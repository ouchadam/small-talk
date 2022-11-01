package app.dapk.st.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.dapk.state.Action
import app.dapk.state.ReducerFactory
import app.dapk.state.Store
import app.dapk.state.createStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class StateViewModel<S, E>(
    reducerFactory: ReducerFactory<S>,
    eventSource: MutableSharedFlow<E>,
) : ViewModel(), State<S, E> {

    private val store: Store<S> = createStore(reducerFactory, viewModelScope)
    override val events: SharedFlow<E> = eventSource
    override val current
        get() = _state!!
    private var _state: S by mutableStateOf(store.getState())

    init {
        _state = store.getState()
        store.subscribe {
            _state = it
        }
    }

    override fun dispatch(action: Action) {
        viewModelScope.launch { store.dispatch(action) }
    }
}

fun <S, E> createStateViewModel(block: (suspend (E) -> Unit) -> ReducerFactory<S>): StateViewModel<S, E> {
    val eventSource = MutableSharedFlow<E>(extraBufferCapacity = 1)
    val reducer = block { eventSource.emit(it) }
    return StateViewModel(reducer, eventSource)
}

interface State<S, E> {
    fun dispatch(action: Action)
    val events: SharedFlow<E>
    val current: S
}
