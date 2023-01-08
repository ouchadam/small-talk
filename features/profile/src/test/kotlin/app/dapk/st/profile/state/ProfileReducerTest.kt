package app.dapk.st.profile.state

import app.dapk.st.core.Lce
import app.dapk.st.engine.Me
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.state.Combined2
import app.dapk.state.SpiderPage
import app.dapk.state.page.PageAction
import app.dapk.state.page.PageContainer
import app.dapk.state.page.PageStateChange
import fake.FakeChatEngine
import fake.FakeErrorTracker
import fake.FakeJobBag
import fixture.aRoomId
import fixture.aRoomInvite
import fixture.aUserId
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import test.assertOnlyDispatches
import test.delegateEmit
import test.testReducer

private val INITIAL_PROFILE_PAGE = SpiderPage(Page.Routes.profile, "Profile", null, Page.Profile(Lce.Loading()), hasToolbar = false)
private val INITIAL_INVITATION_PAGE = SpiderPage(Page.Routes.invitation, "Invitations", Page.Routes.profile, Page.Invitations(Lce.Loading()), hasToolbar = true)
private val A_ROOM_ID = aRoomId()
private val A_PROFILE_CONTENT = Page.Profile.Content(Me(aUserId(), null, null, HomeServerUrl("ignored")), invitationsCount = 4)

class ProfileReducerTest {

    private val fakeChatEngine = FakeChatEngine()
    private val fakeErrorTracker = FakeErrorTracker()
    private val fakeProfileUseCase = FakeProfileUseCase()
    private val fakeJobBag = FakeJobBag()

    private val runReducerTest = testReducer { _: (Unit) -> Unit ->
        profileReducer(
            fakeChatEngine,
            fakeErrorTracker,
            fakeProfileUseCase.instance,
            fakeJobBag.instance,
        )
    }

    @Test
    fun `initial state is empty loading`() = runReducerTest {
        assertInitialState(pageState(SpiderPage(Page.Routes.profile, "Profile", null, Page.Profile(Lce.Loading()), hasToolbar = false)))
    }

    @Test
    fun `given on Profile page, when Reset, then does nothing`() = runReducerTest {
        reduce(ProfileAction.Reset)

        assertNoChanges()
    }

    @Test
    fun `when Visible, then updates Profile page content`() = runReducerTest {
        fakeJobBag.instance.expect { it.replace(Page.Profile::class, any()) }
        fakeProfileUseCase.givenContent().emits(Lce.Content(A_PROFILE_CONTENT))

        reduce(ProfileAction.ComponentLifecycle.Visible)

        assertOnlyDispatches(
            PageStateChange.UpdatePage(INITIAL_PROFILE_PAGE.state.copy(Lce.Content(A_PROFILE_CONTENT))),
        )
    }

    @Test
    fun `when GoToInvitations, then goes to Invitations page and updates content`() = runReducerTest {
        fakeJobBag.instance.expect { it.replace(Page.Invitations::class, any()) }
        val goToInvitations = PageAction.GoTo(INITIAL_INVITATION_PAGE)
        actionSideEffect(goToInvitations) { pageState(goToInvitations.page) }
        val content = listOf(aRoomInvite())
        fakeChatEngine.givenInvites().emits(content)

        reduce(ProfileAction.GoToInvitations)

        assertOnlyDispatches(
            PageAction.GoTo(INITIAL_INVITATION_PAGE),
            PageStateChange.UpdatePage(INITIAL_INVITATION_PAGE.state.copy(Lce.Content(content))),
        )
    }

    @Test
    fun `given on Invitation page, when Reset, then goes to Profile page`() = runReducerTest {
        setState(pageState(INITIAL_INVITATION_PAGE))

        reduce(ProfileAction.Reset)

        assertOnlyDispatches(PageAction.GoTo(INITIAL_PROFILE_PAGE))
    }

    @Test
    fun `when RejectRoomInvite, then rejects room`() = runReducerTest {
        fakeChatEngine.expect { it.rejectRoom(A_ROOM_ID) }

        reduce(ProfileAction.RejectRoomInvite(A_ROOM_ID))
    }

    @Test
    fun `when AcceptRoomInvite, then joins room`() = runReducerTest {
        fakeChatEngine.expect { it.joinRoom(A_ROOM_ID) }

        reduce(ProfileAction.AcceptRoomInvite(A_ROOM_ID))
    }

    @Test
    fun `when ChangePage, then cancels any previous page jobs`() = runReducerTest {
        fakeJobBag.instance.expect { it.cancel(Page.Invitations::class) }

        reduce(PageStateChange.ChangePage(INITIAL_INVITATION_PAGE, INITIAL_PROFILE_PAGE))
    }
}

private fun <P> pageState(page: SpiderPage<out P>) = Combined2(PageContainer(page), Unit)

class FakeProfileUseCase {
    val instance = mockk<ProfileUseCase>()
    fun givenContent() = every { instance.content() }.delegateEmit()
}
