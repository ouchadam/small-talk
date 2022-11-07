package app.dapk.st.profile.state

import app.dapk.st.core.Lce
import app.dapk.st.engine.Me
import app.dapk.st.matrix.common.HomeServerUrl
import fake.FakeChatEngine
import fake.FakeErrorTracker
import fixture.aRoomInvite
import fixture.aUserId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private val A_ME = Me(aUserId(), null, null, HomeServerUrl("ignored"))
private val AN_INVITES_LIST = listOf(aRoomInvite(), aRoomInvite(), aRoomInvite(), aRoomInvite())
private val AN_ERROR = RuntimeException()

class ProfileUseCaseTest {

    private val fakeChatEngine = FakeChatEngine()
    private val fakeErrorTracker = FakeErrorTracker()

    private val useCase = ProfileUseCase(fakeChatEngine, fakeErrorTracker)

    @Test
    fun `given me and invites, when fetching content, then emits content`() = runTest {
        fakeChatEngine.givenMe(forceRefresh = true).returns(A_ME)
        fakeChatEngine.givenInvites().emits(AN_INVITES_LIST)

        val result = useCase.content().first()

        result shouldBeEqualTo Lce.Content(Page.Profile.Content(A_ME, invitationsCount = AN_INVITES_LIST.size))
    }

    @Test
    fun `given me fails, when fetching content, then emits error`() = runTest {
        fakeChatEngine.givenMe(forceRefresh = true).throws(AN_ERROR)
        fakeChatEngine.givenInvites().emits(emptyList())

        val result = useCase.content().first()

        result shouldBeEqualTo Lce.Error(AN_ERROR)
    }
}