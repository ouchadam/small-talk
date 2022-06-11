package app.dapk.st.notifications

import android.graphics.drawable.Icon
import app.dapk.st.core.DeviceMeta
import app.dapk.st.imageloader.IconLoader
import app.dapk.st.matrix.common.AvatarUrl
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.notifications.AndroidNotificationStyle.Inbox
import app.dapk.st.notifications.AndroidNotificationStyle.Messaging
import fixture.NotificationDelegateFixtures.anAndroidPerson
import fixture.NotificationFixtures.aRoomNotification
import fixture.aRoomMember
import fixture.aRoomOverview
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import test.delegateReturn

private val A_GROUP_ROOM_OVERVIEW = aRoomOverview(roomName = "my awesome room", isGroup = true)

class NotificationStyleFactoryTest {

    private val fakeIconLoader = FakeIconLoader()

    private val styleFactory = NotificationStyleFactory(
        fakeIconLoader,
        DeviceMeta(28),
    )

    @Test
    fun `when creating summary style, then creates android framework inbox style`() {
        val result = styleFactory.summary(
            listOf(
                aRoomNotification(summary = "room 1 summary", messageCount = 10),
                aRoomNotification(summary = "room 2 summary", messageCount = 1),
            )
        )

        result shouldBeEqualTo Inbox(
            lines = listOf("room 1 summary", "room 2 summary"),
            summary = "11 messages from 2 chats"
        )
    }

    @Test
    fun `when creating message style, then creates android framework messaging style`() = runTest {
        val aMessage = aNotifiable(author = aRoomMember(displayName = "a display name", avatarUrl = AvatarUrl("a-url")))
        val authorIcon = anIcon()
        fakeIconLoader.given(aMessage.author.avatarUrl!!.value).returns(authorIcon)

        val result = styleFactory.message(listOf(aMessage), A_GROUP_ROOM_OVERVIEW)

        result shouldBeEqualTo Messaging(
            person = Messaging.AndroidPerson(name = "me", key = A_GROUP_ROOM_OVERVIEW.roomId.value, icon = null),
            title = A_GROUP_ROOM_OVERVIEW.roomName,
            isGroup = true,
            content = listOf(aMessage.toAndroidMessage(authorIcon))
        )
    }

}

private fun Notifiable.toAndroidMessage(expectedAuthorIcon: Icon) = Messaging.AndroidMessage(
    anAndroidPerson(
        name = author.displayName!!,
        key = author.id.value,
        icon = expectedAuthorIcon
    ),
    content = content,
    timestamp = utcTimestamp,
)

fun aNotifiable(
    content: String = "notifiable content",
    utcTimestamp: Long = 1000,
    author: RoomMember = aRoomMember()
) = Notifiable(content, utcTimestamp, author)

class FakeIconLoader : IconLoader by mockk() {
    fun given(url: String) = coEvery { load(url) }.delegateReturn()
}

class FakeIcon {
    val instance = mockk<Icon>()
}

fun anIcon() = FakeIcon().instance