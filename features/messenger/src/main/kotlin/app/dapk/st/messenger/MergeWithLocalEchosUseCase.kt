package app.dapk.st.messenger

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.sync.MessageMeta
import app.dapk.st.matrix.sync.RoomEvent
import app.dapk.st.matrix.sync.RoomState

internal typealias MergeWithLocalEchosUseCase = (RoomState, RoomMember, List<MessageService.LocalEcho>) -> RoomState

internal class MergeWithLocalEchosUseCaseImpl : MergeWithLocalEchosUseCase {

    override fun invoke(roomState: RoomState, member: RoomMember, echos: List<MessageService.LocalEcho>): RoomState {
        val echosByEventId = echos.associateBy { it.eventId }
        val stateByEventId = roomState.events.associateBy { it.eventId }

        val uniqueEchos = echos.filter { echo ->
            echo.eventId == null || stateByEventId[echo.eventId] == null
        }.map { localEcho ->
            when (val message = localEcho.message) {
                is MessageService.Message.TextMessage -> {
                    createMessage(localEcho, message, member)
                }
            }
        }

        val existingWithEcho = roomState.events.map {
            when (val echo = echosByEventId[it.eventId]) {
                null -> it
                else -> when (it) {
                    is RoomEvent.Message -> it.copy(
                        meta = echo.toMeta()
                    )
                    is RoomEvent.Reply -> it.copy(message = it.message.copy(meta = echo.toMeta()))
                }
            }
        }
        val sortedEvents = (existingWithEcho + uniqueEchos)
            .sortedByDescending { if (it is RoomEvent.Message) it.utcTimestamp else null }
            .distinctBy { it.eventId }
        return roomState.copy(events = sortedEvents)
    }
}


private fun createMessage(localEcho: MessageService.LocalEcho, message: MessageService.Message.TextMessage, member: RoomMember) = RoomEvent.Message(
    eventId = localEcho.eventId ?: EventId(localEcho.localId),
    content = message.content.body,
    author = member,
    utcTimestamp = message.timestampUtc,
    meta = localEcho.toMeta()
)

private fun MessageService.LocalEcho.toMeta() = MessageMeta.LocalEcho(
    echoId = this.localId,
    state = when (val localEchoState = this.state) {
        MessageService.LocalEcho.State.Sending -> MessageMeta.LocalEcho.State.Sending
        MessageService.LocalEcho.State.Sent -> MessageMeta.LocalEcho.State.Sent
        is MessageService.LocalEcho.State.Error -> MessageMeta.LocalEcho.State.Error(
            localEchoState.message,
            type = MessageMeta.LocalEcho.State.Error.Type.UNKNOWN,
        )
    }
)
