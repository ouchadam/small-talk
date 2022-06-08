package app.dapk.st.domain.localecho

import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.Scope
import app.dapk.db.DapkDb
import app.dapk.db.model.DbLocalEcho
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.LocalEchoStore
import app.dapk.st.matrix.message.MessageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private typealias LocalEchoCache = Map<RoomId, Map<String, MessageService.LocalEcho>>

class LocalEchoPersistence(
    private val errorTracker: ErrorTracker,
    private val database: DapkDb,
) : LocalEchoStore {

    private val inMemoryEchos = MutableStateFlow<LocalEchoCache>(emptyMap())
    private val mirrorScope = Scope(newSingleThreadContext("local-echo-thread"))

    override suspend fun preload() {
        withContext(Dispatchers.IO) {
            val echos = database.localEchoQueries.selectAll().executeAsList().map {
                Json.decodeFromString(MessageService.LocalEcho.serializer(), it.blob)
            }
            inMemoryEchos.value = echos.groupBy {
                when (val message = it.message) {
                    is MessageService.Message.TextMessage -> message.roomId
                    is MessageService.Message.ImageMessage -> message.roomId
                }
            }.mapValues {
                it.value.associateBy {
                    when (val message = it.message) {
                        is MessageService.Message.TextMessage -> message.localId
                        is MessageService.Message.ImageMessage -> message.localId
                    }
                }
            }
        }
    }

    override fun markSending(message: MessageService.Message) {
        emitUpdate(MessageService.LocalEcho(eventId = null, message, state = MessageService.LocalEcho.State.Sending))
    }

    override suspend fun messageTransaction(message: MessageService.Message, action: suspend () -> EventId) {
        emitUpdate(MessageService.LocalEcho(eventId = null, message, state = MessageService.LocalEcho.State.Sending))
        try {
            val eventId = action.invoke()
            emitUpdate(MessageService.LocalEcho(eventId = eventId, message, state = MessageService.LocalEcho.State.Sent))
            database.transaction {
                when (message) {
                    is MessageService.Message.TextMessage -> database.localEchoQueries.delete(message.localId)
                }
            }
        } catch (error: Exception) {
            emitUpdate(
                MessageService.LocalEcho(
                    eventId = null,
                    message,
                    state = MessageService.LocalEcho.State.Error(error.message ?: "", MessageService.LocalEcho.State.Error.Type.UNKNOWN)
                )
            )
            errorTracker.track(error)
            throw error
        }
    }

    private fun emitUpdate(localEcho: MessageService.LocalEcho) {
        val newValue = inMemoryEchos.value.addEcho(localEcho)
        inMemoryEchos.tryEmit(newValue)

        mirrorScope.launch {
            when (val message = localEcho.message) {
                is MessageService.Message.TextMessage -> database.localEchoQueries.insert(
                    DbLocalEcho(
                        message.localId,
                        message.roomId.value,
                        Json.encodeToString(MessageService.LocalEcho.serializer(), localEcho)
                    )
                )
            }
        }
    }

    override fun observeLocalEchos(roomId: RoomId) = inMemoryEchos.map {
        it[roomId]?.values?.toList() ?: emptyList()
    }

    override fun observeLocalEchos() = inMemoryEchos.map {
        it.mapValues { it.value.values.toList() }
    }
}

private fun LocalEchoCache.addEcho(localEcho: MessageService.LocalEcho): MutableMap<RoomId, Map<String, MessageService.LocalEcho>> {
    val newValue = this.toMutableMap()
    val roomEchos = newValue.getOrPut(localEcho.roomId) { emptyMap() }
    newValue[localEcho.roomId] = roomEchos.toMutableMap().also { it.update(localEcho) }
    return newValue
}

private fun MutableMap<String, MessageService.LocalEcho>.update(localEcho: MessageService.LocalEcho) {
    this[localEcho.localId] = localEcho
}
