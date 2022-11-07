package app.dapk.st.profile.state

import app.dapk.st.core.Lce
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ProfileUseCase(
    private val chatEngine: ChatEngine,
    private val errorTracker: ErrorTracker,
) {

    fun content(): Flow<Lce<Page.Profile.Content>> {
        val flow = flow {
            val result = runCatching { chatEngine.me(forceRefresh = true) }
                .onFailure { errorTracker.track(it, "Loading profile") }
            emit(result)
        }
        val combine = combine(flow, chatEngine.invites(), transform = { me, invites -> me to invites })
        return combine.map { (me, invites) ->
            when (me.isSuccess) {
                true -> Lce.Content(Page.Profile.Content(me.getOrThrow(), invites.size))
                false -> Lce.Error(me.exceptionOrNull()!!)
            }
        }
    }
}