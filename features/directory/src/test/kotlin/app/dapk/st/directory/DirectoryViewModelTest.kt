package app.dapk.st.directory

import ViewModelTest
import fixture.aRoomOverview
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import test.delegateReturn

private val AN_OVERVIEW = aRoomOverview()
private val AN_OVERVIEW_STATE = RoomFoo(AN_OVERVIEW, UnreadCount(1), null)

class DirectoryViewModelTest {

    private val runViewModelTest = ViewModelTest()
    private val fakeDirectoryUseCase = FakeDirectoryUseCase()
    private val fakeShortcutHandler = FakeShortcutHandler()

    private val viewModel = DirectoryViewModel(
        fakeShortcutHandler.instance,
        fakeDirectoryUseCase.instance,
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
        fakeDirectoryUseCase.given().returns(flowOf(listOf(AN_OVERVIEW_STATE)))

        viewModel.test().start()

        assertStates(DirectoryScreenState.Content(listOf(AN_OVERVIEW_STATE)))
        verifyExpects()
    }
}

class FakeShortcutHandler {
    val instance = mockk<ShortcutHandler>()
}

class FakeDirectoryUseCase {
    val instance = mockk<DirectoryUseCase>()
    fun given() = every { instance.state() }.delegateReturn()
}