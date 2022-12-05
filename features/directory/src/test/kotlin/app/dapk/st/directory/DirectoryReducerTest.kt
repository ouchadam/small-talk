package app.dapk.st.directory

import app.dapk.st.directory.state.*
import app.dapk.st.engine.UnreadCount
import fake.FakeChatEngine
import fake.FakeJobBag
import fixture.aRoomOverview
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import test.testReducer

private val AN_OVERVIEW = aRoomOverview()
private val AN_OVERVIEW_STATE = app.dapk.st.engine.DirectoryItem(AN_OVERVIEW, UnreadCount(1), null, isMuted = false)

class DirectoryReducerTest {

    private val fakeShortcutHandler = FakeShortcutHandler()
    private val fakeChatEngine = FakeChatEngine()
    private val fakeJobBag = FakeJobBag()

    private val runReducerTest = testReducer { fakeEventSource ->
        directoryReducer(
            fakeChatEngine,
            fakeShortcutHandler.instance,
            fakeJobBag.instance,
            fakeEventSource,
        )
    }

    @Test
    fun `initial state is empty loading`() = runReducerTest {
        assertInitialState(DirectoryScreenState.EmptyLoading)
    }

    @Test
    fun `given directory content, when Visible, then updates shortcuts and dispatches room state`() = runReducerTest {
        fakeShortcutHandler.instance.expectUnit { it.onDirectoryUpdate(listOf(AN_OVERVIEW)) }
        fakeJobBag.instance.expect { it.replace("sync", any()) }
        fakeChatEngine.givenDirectory().returns(flowOf(listOf(AN_OVERVIEW_STATE)))

        reduce(ComponentLifecycle.OnVisible)

        assertOnlyDispatches(listOf(DirectoryStateChange.Content(listOf(AN_OVERVIEW_STATE))))
    }

    @Test
    fun `given no directory content, when Visible, then updates shortcuts and dispatches empty state`() = runReducerTest {
        fakeShortcutHandler.instance.expectUnit { it.onDirectoryUpdate(emptyList()) }
        fakeJobBag.instance.expect { it.replace("sync", any()) }
        fakeChatEngine.givenDirectory().returns(flowOf(emptyList()))

        reduce(ComponentLifecycle.OnVisible)

        assertOnlyDispatches(listOf(DirectoryStateChange.Empty))
    }

    @Test
    fun `when Gone, then cancels sync job`() = runReducerTest {
        fakeJobBag.instance.expect { it.cancel("sync") }

        reduce(ComponentLifecycle.OnGone)

        assertNoChanges()
    }

    @Test
    fun `when ScrollToTop, then emits Scroll event`() = runReducerTest {
        reduce(DirectorySideEffect.ScrollToTop)

        assertOnlyEvents(listOf(DirectoryEvent.ScrollToTop))
    }

    @Test
    fun `when Content StateChange, then returns Content state`() = runReducerTest {
        reduce(DirectoryStateChange.Content(listOf(AN_OVERVIEW_STATE)))

        assertOnlyStateChange(DirectoryScreenState.Content(listOf(AN_OVERVIEW_STATE)))
    }

    @Test
    fun `when Empty StateChange, then returns Empty state`() = runReducerTest {
        reduce(DirectoryStateChange.Empty)

        assertOnlyStateChange(DirectoryScreenState.Empty)
    }
}

internal class FakeShortcutHandler {
    val instance = mockk<ShortcutHandler>()
}
