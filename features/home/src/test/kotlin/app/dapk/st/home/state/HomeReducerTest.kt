package app.dapk.st.home.state

import app.dapk.st.directory.state.ComponentLifecycle
import app.dapk.st.directory.state.DirectorySideEffect
import app.dapk.st.domain.StoreCleaner
import app.dapk.st.engine.Me
import app.dapk.st.home.BetaVersionUpgradeUseCase
import app.dapk.st.matrix.common.HomeServerUrl
import app.dapk.st.profile.state.ProfileAction
import fake.FakeChatEngine
import fake.FakeJobBag
import fixture.aRoomId
import fixture.aRoomInvite
import fixture.aUserId
import io.mockk.mockk
import org.junit.Test
import test.*

private val A_ME = Me(aUserId(), displayName = null, avatarUrl = null, homeServerUrl = HomeServerUrl("ignored"))
private val A_SIGNED_IN_STATE = HomeScreenState.SignedIn(
    HomeScreenState.Page.Directory,
    me = A_ME,
    invites = 0,
)

class HomeReducerTest {

    private val fakeStoreCleaner = FakeStoreCleaner()
    private val fakeChatEngine = FakeChatEngine()
    private val fakeBetaVersionUpgradeUseCase = FakeBetaVersionUpgradeUseCase()
    private val fakeJobBag = FakeJobBag()

    private val runReducerTest = testReducer { fakeEventSource ->
        homeReducer(
            fakeChatEngine,
            fakeStoreCleaner,
            fakeBetaVersionUpgradeUseCase.instance,
            fakeJobBag.instance,
            fakeEventSource,
        )
    }

    @Test
    fun `initial state is loading`() = runReducerTest {
        assertInitialState(HomeScreenState.Loading)
    }

    @Test
    fun `when UpdateState, then replaces state`() = runReducerTest {
        reduce(HomeAction.UpdateState(HomeScreenState.SignedOut))

        assertOnlyStateChange(HomeScreenState.SignedOut)
    }

    @Test
    fun `given SignedIn, when UpdateInviteCount, then updates invite count`() = runReducerTest {
        setState(A_SIGNED_IN_STATE)

        reduce(HomeAction.UpdateInvitesCount(invitesCount = 90))

        assertOnlyStateChange(A_SIGNED_IN_STATE.copy(invites = 90))
    }

    @Test
    fun `when ScrollToTop, then forwards to directory scroll event`() = runReducerTest {
        reduce(HomeAction.ScrollToTop)

        assertOnlyDispatches(DirectorySideEffect.ScrollToTop)
    }

    @Test
    fun `when ClearCache, then clears store cache, upgrades and relaunches`() = runReducerTest {
        fakeStoreCleaner.expect { it.cleanCache(removeCredentials = false) }
        fakeBetaVersionUpgradeUseCase.instance.expect { it.notifyUpgraded() }

        reduce(HomeAction.ClearCache)

        assertOnlyEvents(HomeEvent.Relaunch)
    }

    @Test
    fun `given SignedIn and invites update, when Visible, then show content and update on invite changes`() = runReducerTest {
        fakeChatEngine.givenIsSignedIn().returns(true)

        reduce(HomeAction.LifecycleVisible)

        assertEvents(HomeEvent.OnShowContent)
        assertDispatches(HomeAction.InitialHome)
        assertNoStateChange()
    }

    @Test
    fun `given SignedOut and invites update, when Visible, then show content and update on invite changes`() = runReducerTest {
        fakeChatEngine.givenIsSignedIn().returns(false)

        reduce(HomeAction.LifecycleVisible)

        assertOnlyDispatches(HomeAction.UpdateState(HomeScreenState.SignedOut))
    }

    @Test
    fun `given SignedIn, when InitialHome, then updates me state and listens to invite changes`() = runReducerTest {
        setState(A_SIGNED_IN_STATE)
        fakeChatEngine.givenMe(forceRefresh = false).returns(A_ME)
        givenInvites(count = 5)

        reduce(HomeAction.InitialHome)

        assertOnlyDispatches(
            HomeAction.UpdateToSignedIn(A_ME),
            HomeAction.UpdateInvitesCount(5)
        )
    }

    @Test
    fun `given SignedIn, when UpdateToSignedIn, then updates me state`() = runReducerTest {
        setState(A_SIGNED_IN_STATE)
        val expectedMe = A_ME.copy(aUserId("another-user"))

        reduce(HomeAction.UpdateToSignedIn(expectedMe))

        assertOnlyStateChange(A_SIGNED_IN_STATE.copy(me = expectedMe))
    }

    @Test
    fun `given Loading, when UpdateToSignedIn, then set SignedIn and updates me state`() = runReducerTest {
        setState(HomeScreenState.Loading)
        val expectedMe = A_ME.copy(aUserId("another-user"))

        reduce(HomeAction.UpdateToSignedIn(expectedMe))

        assertOnlyStateChange(A_SIGNED_IN_STATE.copy(me = expectedMe))
    }

    @Test
    fun `given SignedOut, when UpdateToSignedIn, then set SignedIn and updates me state`() = runReducerTest {
        setState(HomeScreenState.SignedOut)
        val expectedMe = A_ME.copy(aUserId("another-user"))

        reduce(HomeAction.UpdateToSignedIn(expectedMe))

        assertOnlyStateChange(A_SIGNED_IN_STATE.copy(me = expectedMe))
    }

    @Test
    fun `when LoggedIn, then emit show content and fetch initial home`() = runReducerTest {
        setState(HomeScreenState.SignedOut)
        givenInvites(count = 0)

        reduce(HomeAction.LoggedIn)

        assertDispatches(HomeAction.InitialHome)
        assertEvents(HomeEvent.OnShowContent)
        assertNoStateChange()
    }

    @Test
    fun `given SignedOut, when ChangePage, then does nothing`() = runReducerTest {
        setState(HomeScreenState.SignedOut)

        reduce(HomeAction.ChangePage(HomeScreenState.Page.Directory))

        assertNoChanges()
    }

    @Test
    fun `given Loading, when ChangePage, then does nothing`() = runReducerTest {
        setState(HomeScreenState.Loading)

        reduce(HomeAction.ChangePage(HomeScreenState.Page.Directory))

        assertNoChanges()
    }

    @Test
    fun `given SignedIn, when ChangePage to same page, then does nothing`() = runReducerTest {
        val page = HomeScreenState.Page.Directory
        setState(A_SIGNED_IN_STATE.copy(page = page))

        reduce(HomeAction.ChangePage(page))

        assertNoChanges()
    }

    @Test
    fun `given SignedIn, when ChangePage to different page, then updates page and emits side effect`() = runReducerTest {
        val expectedPage = HomeScreenState.Page.Profile
        setState(A_SIGNED_IN_STATE.copy(page = HomeScreenState.Page.Directory))

        reduce(HomeAction.ChangePage(expectedPage))

        assertStateChange(A_SIGNED_IN_STATE.copy(page = expectedPage))
        assertDispatches(HomeAction.ChangePageSideEffect(expectedPage))
    }

    @Test
    fun `when ChangePageSide is Directory, then does nothing`() = runReducerTest {
        reduce(HomeAction.ChangePageSideEffect(HomeScreenState.Page.Directory))

        assertNoChanges()
    }

    @Test
    fun `when ChangePageSide is Profile, then mark directory gone and resets profile`() = runReducerTest {
        reduce(HomeAction.ChangePageSideEffect(HomeScreenState.Page.Profile))

        assertOnlyDispatches(
            ComponentLifecycle.OnGone,
            ProfileAction.Reset
        )
    }

    private fun givenInvites(count: Int) {
        fakeJobBag.instance.expect { it.replace("invites-count", any()) }
        val invites = List(count) { aRoomInvite(roomId = aRoomId(it.toString())) }
        fakeChatEngine.givenInvites().emits(invites)
    }
}

class FakeStoreCleaner : StoreCleaner by mockk()

class FakeBetaVersionUpgradeUseCase {
    val instance = mockk<BetaVersionUpgradeUseCase>()
}