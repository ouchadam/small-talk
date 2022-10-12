package app.dapk.st.directory

import ViewModelTest
import app.dapk.st.engine.DirectoryItem
import app.dapk.st.engine.UnreadCount
import fake.FakeChatEngine
import fixture.aRoomOverview
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

private val AN_OVERVIEW = aRoomOverview()
private val AN_OVERVIEW_STATE = DirectoryItem(AN_OVERVIEW, UnreadCount(1), null)

class DirectoryViewModelTest {

    private val runViewModelTest = ViewModelTest()
    private val fakeShortcutHandler = FakeShortcutHandler()
    private val fakeChatEngine = FakeChatEngine()

    private val viewModel = DirectoryViewModel(
        fakeShortcutHandler.instance,
        fakeChatEngine,
        runViewModelTest.testMutableStateFactory(),
    )

    @Test
    fun `when creating view model, then initial state is empty loading`() = runViewModelTest {
        viewModel.test()

        assertInitialState(DirectoryScreenState.EmptyLoading)
    }

    @Test
    fun `when starting, then updates shortcuts and emits room state`() = runViewModelTest {
        fakeShortcutHandler.instance.expectUnit { it.onDirectoryUpdate(listOf(AN_OVERVIEW)) }
        fakeChatEngine.givenDirectory().returns(flowOf(listOf(AN_OVERVIEW_STATE)))

        viewModel.test().start()

        assertStates(DirectoryScreenState.Content(listOf(AN_OVERVIEW_STATE)))
        verifyExpects()
    }
}

class FakeShortcutHandler {
    val instance = mockk<ShortcutHandler>()
}