package app.dapk.st.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

abstract class DapkViewModel<S, VE>(
    initialState: S
) : ViewModel() {

    protected val _events = MutableSharedFlow<VE>(extraBufferCapacity = 1)
    val events: SharedFlow<VE> = _events
    var state by mutableStateOf<S>(initialState)
        protected set

    fun updateState(reducer: S.() -> S) {
        state = reducer(state)
    }
}