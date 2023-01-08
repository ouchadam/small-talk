package app.dapk.st.profile.state

import app.dapk.st.core.JobBag
import app.dapk.st.core.Lce
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.engine.ChatEngine
import app.dapk.state.SpiderPage
import app.dapk.state.async
import app.dapk.state.createReducer
import app.dapk.state.page.PageAction
import app.dapk.state.page.PageStateChange
import app.dapk.state.page.createPageReducer
import app.dapk.state.page.withPageContext
import app.dapk.state.sideEffect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun profileReducer(
    chatEngine: ChatEngine,
    errorTracker: ErrorTracker,
    profileUseCase: ProfileUseCase,
    jobBag: JobBag,
) = createPageReducer(
    initialPage = SpiderPage<Page>(Page.Routes.profile, "Profile", null, Page.Profile(Lce.Loading()), hasToolbar = false),
    factory = {
        createReducer(
            initialState = Unit,

            async(ProfileAction.ComponentLifecycle::class) {
                when (it) {
                    ProfileAction.ComponentLifecycle.Visible -> {
                        jobBag.replace(Page.Profile::class, profileUseCase.content().onEach { content ->
                            withPageContext<Page.Profile> {
                                pageDispatch(PageStateChange.UpdatePage(it.copy(content = content)))
                            }
                        }.launchIn(coroutineScope))
                    }

                    ProfileAction.ComponentLifecycle.Gone -> jobBag.cancelAll()
                }
            },

            async(ProfileAction.GoToInvitations::class) {
                dispatch(PageAction.GoTo(SpiderPage(Page.Routes.invitation, "Invitations", Page.Routes.profile, Page.Invitations(Lce.Loading()))))

                jobBag.replace(Page.Invitations::class, chatEngine.invites()
                    .onEach { invitations ->
                        withPageContext<Page.Invitations> {
                            pageDispatch(PageStateChange.UpdatePage(it.copy(content = Lce.Content(invitations))))
                        }
                    }
                    .launchIn(coroutineScope))

            },

            async(ProfileAction.Reset::class) {
                when (rawPage().state) {
                    is Page.Invitations -> {
                        dispatch(PageAction.GoTo(SpiderPage<Page>(Page.Routes.profile, "Profile", null, Page.Profile(Lce.Loading()), hasToolbar = false)))
                    }

                    is Page.Profile -> {
                        // do nothing
                    }
                }
            },

            sideEffect(ProfileAction.AcceptRoomInvite::class) { action, _ ->
                kotlin.runCatching { chatEngine.joinRoom(action.roomId) }.fold(
                    onFailure = { errorTracker.track(it) },
                    onSuccess = {}
                )
            },

            sideEffect(ProfileAction.RejectRoomInvite::class) { action, _ ->
                kotlin.runCatching { chatEngine.rejectRoom(action.roomId) }.fold(
                    onFailure = { errorTracker.track(it) },
                    onSuccess = {}
                )
            },

            sideEffect(PageStateChange.ChangePage::class) { action, _ ->
                jobBag.cancel(action.previous.state::class)
            },
        )
    }
)
