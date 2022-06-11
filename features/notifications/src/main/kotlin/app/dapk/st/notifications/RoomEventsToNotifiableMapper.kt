package app.dapk.st.notifications

import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.sync.RoomEvent

class RoomEventsToNotifiableMapper {

    fun map(events: List<RoomEvent>): List<Notifiable> {
        return events.map {
            when (it) {
                is RoomEvent.Image -> Notifiable(content = it.toNotifiableContent(), it.utcTimestamp, it.author)
                is RoomEvent.Message -> Notifiable(content = it.toNotifiableContent(), it.utcTimestamp, it.author)
                is RoomEvent.Reply -> Notifiable(content = it.toNotifiableContent(), it.utcTimestamp, it.author)
            }
        }
    }

    private fun RoomEvent.toNotifiableContent(): String = when (this) {
        is RoomEvent.Image -> "\uD83D\uDCF7"
        is RoomEvent.Message -> this.content
        is RoomEvent.Reply -> this.message.toNotifiableContent()
    }

}

data class Notifiable(val content: String, val utcTimestamp: Long, val author: RoomMember)
