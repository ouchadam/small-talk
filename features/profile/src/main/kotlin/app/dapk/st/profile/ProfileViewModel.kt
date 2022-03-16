package app.dapk.st.profile

import androidx.lifecycle.viewModelScope
import app.dapk.st.core.Lce
import app.dapk.st.design.components.SpiderPage
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.room.ProfileService
import app.dapk.st.matrix.room.RoomService
import app.dapk.st.matrix.sync.SyncService
import app.dapk.st.viewmodel.DapkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileService: ProfileService,
    private val syncService: SyncService,
    private val roomService: RoomService,
) : DapkViewModel<ProfileScreenState, ProfileEvent>(
    ProfileScreenState(SpiderPage(Page.Routes.profile, "Profile", null, Page.Profile(Lce.Loading()), hasToolbar = false))
) {

    private var syncingJob: Job? = null
    private var currentPageJob: Job? = null

    fun start() {
        goToProfile()
    }

    private fun goToProfile() {
        syncingJob = syncService.startSyncing().launchIn(viewModelScope)

        combine(
            flow { emit(profileService.me(forceRefresh = true)) },
            syncService.invites(),
            transform = { me, invites -> me to invites }
        )
            .onEach { (me, invites) ->
                updatePageState<Page.Profile> {
                    copy(content = Lce.Content(Page.Profile.Content(me, invites.size)))
                }
            }
            .launchPageJob()
    }


    fun goToInvitations() {
        updateState { copy(page = SpiderPage(Page.Routes.invitation, "Invitations", Page.Routes.profile, Page.Invitations(Lce.Loading()))) }

        syncService.invites()
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
        viewModelScope.launch {
            roomService.joinRoom(roomId)
        }
    }

    fun rejectRoomInvite(roomId: RoomId) {
        viewModelScope.launch {
            roomService.rejectJoinRoom(roomId)
        }
    }

    fun stop() {
        syncingJob?.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified S : Page> updatePageState(crossinline block: S.() -> S) {
        val page = state.page
        val currentState = page.state
        require(currentState is S)
        updateState { copy(page = (page as SpiderPage<S>).copy(state = block(page.state))) }
    }

}
