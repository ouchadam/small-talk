package app.dapk.st.directory

import app.dapk.st.core.StateStore
import app.dapk.st.engine.ChatEngine
import app.dapk.st.engine.DirectoryState
import app.dapk.state.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

typealias DirectoryViewModel = StateStore<DirectoryScreenState, DirectoryEvent>

fun directoryReducer(
    chatEngine: ChatEngine,
    shortcutHandler: ShortcutHandler,
    eventEmitter: suspend (DirectoryEvent) -> Unit,
): ReducerFactory<DirectoryScreenState> {
    var syncJob: Job? = null
    return createReducer(
        initialState = DirectoryScreenState.EmptyLoading,
        multi(ComponentLifecycle::class) { action ->
            when (action) {
                ComponentLifecycle.OnVisible -> async { _ ->
                    syncJob = chatEngine.directory().onEach {
                        shortcutHandler.onDirectoryUpdate(it.map { it.overview })
                        when (it.isEmpty()) {
                            true -> dispatch(DirectoryStateChange.Empty)
                            false -> dispatch(DirectoryStateChange.Content(it))
                        }
                    }.launchIn(coroutineScope)
                }

                ComponentLifecycle.OnGone -> sideEffect { _, _ -> syncJob?.cancel() }
            }
        },
        change(DirectoryStateChange::class) { action, _ ->
            when (action) {
                is DirectoryStateChange.Content -> DirectoryScreenState.Content(action.content)
                DirectoryStateChange.Empty -> DirectoryScreenState.Empty
            }
        },
        sideEffect(DirectorySideEffect.ScrollToTop::class) { _, _ ->
            eventEmitter(DirectoryEvent.ScrollToTop)
        }
    )
}

sealed interface ComponentLifecycle : Action {
    object OnVisible : ComponentLifecycle
    object OnGone : ComponentLifecycle
}

sealed interface DirectorySideEffect : Action {
    object ScrollToTop : DirectorySideEffect
}

sealed interface DirectoryStateChange : Action {
    object Empty : DirectoryStateChange
    data class Content(val content: DirectoryState) : DirectoryStateChange
}
