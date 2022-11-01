package app.dapk.st.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

typealias MutableStateFactory <S> = (S) -> MutableState<S>

fun <S> defaultStateFactory(): MutableStateFactory<S> = { mutableStateOf(it) }

@Suppress("PropertyName")
abstract class DapkViewModel<S, VE>(initialState: S, factory: MutableStateFactory<S> = defaultStateFactory()) : ViewModel() {

    protected val _events = MutableSharedFlow<VE>(extraBufferCapacity = 1)
    val events: SharedFlow<VE> = _events

    var state by factory(initialState)
        protected set

    fun updateState(reducer: S.() -> S) {
        state = reducer(state)
    }
}
