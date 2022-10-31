package app.dapk.st.notifications

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.asString
import fixture.aRoomImageMessageEvent
import fixture.aRoomMessageEvent
import fixture.aRoomReplyMessageEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class RoomEventsToNotifiableMapperTest {

    private val mapper = RoomEventsToNotifiableMapper()

    @Test
    fun `given message event, when mapping, then uses original content`() {
        val event = aRoomMessageEvent()

        val result = mapper.map(listOf(event))

        result shouldBeEqualTo listOf(
            Notifiable(
                content = event.content.asString(),
                utcTimestamp = event.utcTimestamp,
                author = event.author
            )
        )
    }

    @Test
    fun `given image event, when mapping, then replaces content with camera emoji`() {
        val event = aRoomImageMessageEvent()

        val result = mapper.map(listOf(event))

        result shouldBeEqualTo listOf(
            Notifiable(
                content = "📷",
                utcTimestamp = event.utcTimestamp,
                author = event.author
            )
        )
    }

    @Test
    fun `given reply event with message, when mapping, then uses message for content`() {
        val reply = aRoomMessageEvent(utcTimestamp = -1, content = RichText.of("hello"))
        val event = aRoomReplyMessageEvent(reply, replyingTo = aRoomImageMessageEvent(utcTimestamp = -1))

        val result = mapper.map(listOf(event))

        result shouldBeEqualTo listOf(
            Notifiable(
                content = reply.content.asString(),
                utcTimestamp = event.utcTimestamp,
                author = event.author
            )
        )
    }

    @Test
    fun `given reply event with image, when mapping, then uses camera emoji for content`() {
        val event = aRoomReplyMessageEvent(aRoomImageMessageEvent(utcTimestamp = -1))

        val result = mapper.map(listOf(event))

        result shouldBeEqualTo listOf(
            Notifiable(
                content = "📷",
                utcTimestamp = event.utcTimestamp,
                author = event.author
            )
        )
    }
}