package app.dapk.st.directory

import app.dapk.st.directory.state.*
import app.dapk.st.engine.DirectoryItem
import app.dapk.st.engine.UnreadCount
import fake.FakeChatEngine
import fixture.aRoomOverview
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import test.expect

private val AN_OVERVIEW = aRoomOverview()
private val AN_OVERVIEW_STATE = DirectoryItem(AN_OVERVIEW, UnreadCount(1), null)

class DirectoryReducerTest {

    private val fakeShortcutHandler = FakeShortcutHandler()
    private val fakeChatEngine = FakeChatEngine()
    private val fakeJobBag = FakeJobBag()
    private val fakeEventSource = FakeEventSource<DirectoryEvent>()

    private val reducer = directoryReducer(
        fakeChatEngine,
        fakeShortcutHandler.instance,
        fakeJobBag.instance,
        fakeEventSource,
    )

    @Test
    fun `initial state is empty loading`() = runReducerTest(reducer) {
        assertInitialState(DirectoryScreenState.EmptyLoading)
    }

    @Test
    fun `given directory content, when Visible, then updates shortcuts and dispatches room state`() = runReducerTest(reducer) {
        fakeShortcutHandler.instance.expectUnit { it.onDirectoryUpdate(listOf(AN_OVERVIEW)) }
        fakeJobBag.instance.expect { it.add("sync", any()) }
        fakeChatEngine.givenDirectory().returns(flowOf(listOf(AN_OVERVIEW_STATE)))

        reduce(ComponentLifecycle.OnVisible)

        assertNoStateChange()
        assertDispatches(listOf(DirectoryStateChange.Content(listOf(AN_OVERVIEW_STATE))))
    }

    @Test
    fun `given no directory content, when Visible, then updates shortcuts and dispatches empty state`() = runReducerTest(reducer) {
        fakeShortcutHandler.instance.expectUnit { it.onDirectoryUpdate(emptyList()) }
        fakeJobBag.instance.expect { it.add("sync", any()) }
        fakeChatEngine.givenDirectory().returns(flowOf(emptyList()))

        reduce(ComponentLifecycle.OnVisible)

        assertNoStateChange()
        assertDispatches(listOf(DirectoryStateChange.Empty))
    }

    @Test
    fun `when Gone, then cancels sync job`() = runReducerTest(reducer) {
        fakeJobBag.instance.expect { it.cancel("sync") }

        reduce(ComponentLifecycle.OnGone)

        assertNoStateChange()
        assertNoDispatches()
    }

    @Test
    fun `given ScrollToTop, then emits Scroll event`() = runReducerTest(reducer) {
        reduce(DirectorySideEffect.ScrollToTop)

        assertNoStateChange()
        assertNoDispatches()
        fakeEventSource.assertEvents(listOf(DirectoryEvent.ScrollToTop))
    }
}

internal class FakeShortcutHandler {
    val instance = mockk<ShortcutHandler>()
}

class FakeJobBag {
    val instance = mockk<JobBag>()
}
