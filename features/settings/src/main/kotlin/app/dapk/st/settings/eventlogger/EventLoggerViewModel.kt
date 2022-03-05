package app.dapk.st.settings.eventlogger

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.domain.eventlog.EventLogPersistence
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EventLoggerViewModel(
    private val persistence: EventLogPersistence
) : DapkViewModel<EventLoggerState, Unit>(
    initialState = EventLoggerState(
        logs = Lce.Loading(),
        selectedState = null,
    )
) {

    private var logObserverJob: Job? = null

    fun start() {
        viewModelScope.launch {
            updateState { copy(logs = Lce.Loading()) }
            val days = persistence.days()
            updateState { copy(logs = Lce.Content(days)) }
        }
    }

    fun selectLog(logKey: String, filter: String?) {
        logObserverJob?.cancel()
        updateState { copy(selectedState = SelectedState(selectedPage = logKey, content = Lce.Loading(), filter = filter)) }

        logObserverJob = viewModelScope.launch {
            persistence.latest(logKey, filter)
                .onEach {
                    updateState { copy(selectedState = selectedState?.copy(content = Lce.Content(it))) }
                }.collect()
        }
    }

    override fun onCleared() {
        logObserverJob?.cancel()
    }

    fun exitLog() {
        logObserverJob?.cancel()
        updateState { copy(selectedState = null) }
    }
}
