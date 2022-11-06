package app.dapk.st.engine

import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.message.MessageService

internal typealias TimelineMergeWithLocalEchosUseCase = (RoomState, RoomMember, List<MessageService.LocalEcho>) -> RoomState

internal class TimelineMergeWithLocalEchosUseCaseImpl(
    private val localEventMapper: LocalEchoMapper,
) : TimelineMergeWithLocalEchosUseCase {

    override fun invoke(roomState: RoomState, member: RoomMember, echos: List<MessageService.LocalEcho>): RoomState {
        val echosByEventId = echos.associateBy { it.eventId }
        val stateByEventId = roomState.events.associateBy { it.eventId }

        val uniqueEchos = uniqueEchos(echos, stateByEventId, member)
        val existingWithEcho = updateExistingEventsWithLocalEchoMeta(roomState, echosByEventId)

        val sortedEvents = (existingWithEcho + uniqueEchos)
            .sortedByDescending { it.utcTimestamp }
            .distinctBy { it.eventId }
        return roomState.copy(events = sortedEvents)
    }

    private fun uniqueEchos(echos: List<MessageService.LocalEcho>, stateByEventId: Map<EventId, RoomEvent>, member: RoomMember): List<RoomEvent> {
        return with(localEventMapper) {
            echos
                .filter { echo -> echo.eventId == null || stateByEventId[echo.eventId] == null }
                .map { localEcho -> localEcho.toMessage(member) }
        }
    }

    private fun updateExistingEventsWithLocalEchoMeta(roomState: RoomState, echosByEventId: Map<EventId?, MessageService.LocalEcho>): List<RoomEvent> {
        return with(localEventMapper) {
            roomState.events.map { roomEvent ->
                when (val echo = echosByEventId[roomEvent.eventId]) {
                    null -> roomEvent
                    else -> roomEvent.mergeWith(echo)
                }
            }
        }
    }
}
