package app.dapk.st.notifications

import app.dapk.st.engine.RoomEvent
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.asString

class RoomEventsToNotifiableMapper {

    fun map(events: List<RoomEvent>): List<Notifiable> {
        return events.map { Notifiable(content = it.toNotifiableContent(), it.utcTimestamp, it.author) }
    }

    private fun RoomEvent.toNotifiableContent(): String = when (this) {
        is RoomEvent.Image -> "\uD83D\uDCF7"
        is RoomEvent.Message -> this.content.asString()
        is RoomEvent.Reply -> this.message.toNotifiableContent()
        is RoomEvent.Encrypted -> "Encrypted message"
        is RoomEvent.Redacted -> "Deleted message"
    }

}

data class Notifiable(val content: String, val utcTimestamp: Long, val author: RoomMember)
