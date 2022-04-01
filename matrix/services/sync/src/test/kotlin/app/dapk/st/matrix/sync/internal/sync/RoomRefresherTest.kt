package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomState
import fake.FakeMatrixLogger
import fake.FakeRoomDataSource
import internalfake.FakeRoomEventsDecrypter
import fixture.*
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.expect

private val A_ROOM_ID = aRoomId()

private object ARoom {
    val MESSAGE_EVENT = aRoomMessageEvent(utcTimestamp = 0)
    val ENCRYPTED_EVENT = anEncryptedRoomMessageEvent(utcTimestamp = 1)
    val DECRYPTED_EVENT = aRoomMessageEvent(utcTimestamp = 2)
    val PREVIOUS_STATE = RoomState(aRoomOverview(), listOf(MESSAGE_EVENT, ENCRYPTED_EVENT))
    val DECRYPTED_EVENTS = listOf(MESSAGE_EVENT, DECRYPTED_EVENT)
    val NEW_STATE = RoomState(aRoomOverview(lastMessage = DECRYPTED_EVENT.asLastMessage()), DECRYPTED_EVENTS)
}
private val A_USER_CREDENTIALS = aUserCredentials()

internal class RoomRefresherTest {

    private val fakeRoomDataSource = FakeRoomDataSource()
    private val fakeRoomEventsDecrypter = FakeRoomEventsDecrypter()

    private val roomRefresher = RoomRefresher(
        fakeRoomDataSource.instance,
        fakeRoomEventsDecrypter.instance,
        FakeMatrixLogger(),
    )

    @Test
    fun `given no existing room when refreshing then does nothing`() = runTest {
        fakeRoomDataSource.givenNoCachedRoom(A_ROOM_ID)

        val result = roomRefresher.refreshRoomContent(aRoomId(), A_USER_CREDENTIALS)

        result shouldBeEqualTo null
        fakeRoomDataSource.verifyNoChanges()
    }

    @Test
    fun `given existing room when refreshing then processes existing state`() = runTest {
        fakeRoomDataSource.expect { it.instance.persist(RoomId(any()), any(), any()) }
        fakeRoomDataSource.givenRoom(A_ROOM_ID, ARoom.PREVIOUS_STATE)
        fakeRoomEventsDecrypter.givenDecrypts(A_USER_CREDENTIALS, ARoom.PREVIOUS_STATE.events, ARoom.DECRYPTED_EVENTS)

        val result = roomRefresher.refreshRoomContent(aRoomId(), A_USER_CREDENTIALS)

        fakeRoomDataSource.verifyRoomUpdated(ARoom.PREVIOUS_STATE, ARoom.NEW_STATE)
        result shouldBeEqualTo ARoom.NEW_STATE
    }
}

private fun RoomEvent.Message.asLastMessage() = aLastMessage(
    this.content,
    this.utcTimestamp,
    this.author,
)
