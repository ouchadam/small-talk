package app.dapk.st.directory.state

import app.dapk.st.core.JobBag
import app.dapk.st.directory.ShortcutHandler
import app.dapk.st.engine.ChatEngine
import app.dapk.state.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val KEY_SYNCING_JOB = "sync"

internal fun directoryReducer(
    chatEngine: ChatEngine,
    shortcutHandler: ShortcutHandler,
    jobBag: JobBag,
    eventEmitter: suspend (DirectoryEvent) -> Unit,
): ReducerFactory<DirectoryScreenState> {
    return createReducer(
        initialState = DirectoryScreenState.EmptyLoading,

        multi(ComponentLifecycle::class) { action ->
            when (action) {
                ComponentLifecycle.OnVisible -> async { _ ->
                    jobBag.replace(KEY_SYNCING_JOB, chatEngine.directory().onEach {
                        shortcutHandler.onDirectoryUpdate(it.map { it.overview })
                        when (it.isEmpty()) {
                            true -> dispatch(DirectoryStateChange.Empty)
                            false -> dispatch(DirectoryStateChange.Content(it))
                        }
                    }.launchIn(coroutineScope))
                }

                ComponentLifecycle.OnGone -> sideEffect { jobBag.cancel(KEY_SYNCING_JOB) }
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
