package app.dapk.st.engine

import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.RoomMember
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.common.asString
import app.dapk.st.matrix.message.MessageService
import app.dapk.st.matrix.room.RoomService

internal typealias DirectoryMergeWithLocalEchosUseCase = suspend (OverviewState, UserId, Map<RoomId, List<MessageService.LocalEcho>>) -> OverviewState

internal class DirectoryMergeWithLocalEchosUseCaseImpl(
    private val roomService: RoomService,
) : DirectoryMergeWithLocalEchosUseCase {

    override suspend fun invoke(overview: OverviewState, selfId: UserId, echos: Map<RoomId, List<MessageService.LocalEcho>>): OverviewState {
        return when {
            echos.isEmpty() -> overview
            else -> overview.map {
                when (val roomEchos = echos[it.roomId]) {
                    null -> it
                    else -> it.mergeWithLocalEchos(
                        member = roomService.findMember(it.roomId, selfId) ?: RoomMember(
                            selfId,
                            null,
                            avatarUrl = null,
                        ),
                        echos = roomEchos,
                    )
                }
            }
        }
    }

    private fun RoomOverview.mergeWithLocalEchos(member: RoomMember, echos: List<MessageService.LocalEcho>): RoomOverview {
        val latestEcho = echos.maxByOrNull { it.timestampUtc }
        return if (latestEcho != null && latestEcho.timestampUtc > (this.lastMessage?.utcTimestamp ?: 0)) {
            this.copy(
                lastMessage = RoomOverview.LastMessage(
                    content = when (val message = latestEcho.message) {
                        is MessageService.Message.TextMessage -> message.content.body.asString()
                        is MessageService.Message.ImageMessage -> "\uD83D\uDCF7"
                    },
                    utcTimestamp = latestEcho.timestampUtc,
                    author = member,
                )
            )
        } else {
            this
        }
    }

}