package app.dapk.st.profile

import android.util.Log
import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.engine.ChatEngine
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val chatEngine: ChatEngine,
    private val errorTracker: ErrorTracker,
) : DapkViewModel<ProfileScreenState, ProfileEvent>(
    ProfileScreenState(SpiderPage(Page.Routes.profile, "Profile", null, Page.Profile(Lce.Loading()), hasToolbar = false))
) {

    private var currentPageJob: Job? = null

    fun start() {
        goToProfile()
    }

    private fun goToProfile() {
        combine(
            flow {
                val result = runCatching { chatEngine.me(forceRefresh = true) }
                    .onFailure { errorTracker.track(it, "Loading profile") }
                emit(result)
            },
            chatEngine.invites(),
            transform = { me, invites -> me to invites }
        )
            .onEach { (me, invites) ->
                updatePageState<Page.Profile> {
                    when (me.isSuccess) {
                        true -> copy(content = Lce.Content(Page.Profile.Content(me.getOrThrow(), invites.size)))
                        false -> copy(content = Lce.Error(me.exceptionOrNull()!!))
                    }
                }
            }
            .launchPageJob()
    }


    fun goToInvitations() {
        updateState { copy(page = SpiderPage(Page.Routes.invitation, "Invitations", Page.Routes.profile, Page.Invitations(Lce.Loading()))) }

        chatEngine.invites()
            .onEach {
                updatePageState<Page.Invitations> {
                    copy(content = Lce.Content(it))
                }
            }
            .launchPageJob()
    }

    fun goTo(page: SpiderPage<out Page>) {
        currentPageJob?.cancel()
        updateState { copy(page = page) }
        when (page.state) {
            is Page.Invitations -> goToInvitations()
            is Page.Profile -> goToProfile()
        }
    }

    private fun <T> Flow<T>.launchPageJob() {
        currentPageJob?.cancel()
        currentPageJob = this.launchIn(viewModelScope)
    }

    fun updateDisplayName() {
        // TODO
    }

    fun updateAvatar() {
        // TODO
    }

    fun acceptRoomInvite(roomId: RoomId) {
        launchCatching { chatEngine.joinRoom(roomId) }.fold(
            onError = {}
        )
    }

    fun rejectRoomInvite(roomId: RoomId) {
        launchCatching { chatEngine.rejectJoinRoom(roomId) }.fold(
            onError = {
                Log.e("!!!", it.message, it)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : Page> updatePageState(crossinline block: S.() -> S) {
        val page = state.page
        val currentState = page.state
        require(currentState is S)
        updateState { copy(page = (page as SpiderPage<S>).copy(state = block(page.state))) }
    }

    fun reset() {
        when (state.page.state) {
            is Page.Invitations -> updateState {
                ProfileScreenState(
                    SpiderPage(
                        Page.Routes.profile,
                        "Profile",
                        null,
                        Page.Profile(Lce.Loading()),
                        hasToolbar = false
                    )
                )
            }

            is Page.Profile -> {
                // do nothing
            }
        }
    }

    fun stop() {
        currentPageJob?.cancel()
    }

}

fun <S, VE, T> DapkViewModel<S, VE>.launchCatching(block: suspend () -> T): LaunchCatching<T> {
    return object : LaunchCatching<T> {
        override fun fold(onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) {
            viewModelScope.launch { runCatching { block() }.fold(onSuccess, onError) }
        }
    }
}

interface LaunchCatching<T> {
    fun fold(onSuccess: (T) -> Unit = {}, onError: (Throwable) -> Unit = {})
}