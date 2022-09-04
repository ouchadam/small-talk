package app.dapk.st.domain

import app.dapk.db.DapkDb
import app.dapk.db.model.DbCryptoAccount
import app.dapk.db.model.DbCryptoMegolmInbound
import app.dapk.db.model.DbCryptoMegolmOutbound
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.Curve25519
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SessionId
import com.squareup.sqldelight.TransactionWithoutReturn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OlmPersistence(
    private val database: DapkDb,
    private val credentialsStore: CredentialsStore,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend fun read(): String? {
        return dispatchers.withIoContext {
            database.cryptoQueries
                .selectAccount(credentialsStore.credentials()!!.userId.value)
                .executeAsOneOrNull()
        }
    }

    suspend fun persist(olmAccount: SerializedObject) {
        dispatchers.withIoContext {
            database.cryptoQueries.insertAccount(
                DbCryptoAccount(
                    user_id = credentialsStore.credentials()!!.userId.value,
                    blob = olmAccount.value
                )
            )
        }
    }

    suspend fun readOutbound(roomId: RoomId): Pair<Long, String>? {
        return dispatchers.withIoContext {
            database.cryptoQueries
                .selectMegolmOutbound(roomId.value)
                .executeAsOneOrNull()?.let {
                    it.utcEpochMillis to it.blob
                }
        }
    }

    suspend fun persistOutbound(roomId: RoomId, creationTimestampUtc: Long, outboundGroupSession: SerializedObject) {
        dispatchers.withIoContext {
            database.cryptoQueries.insertMegolmOutbound(
                DbCryptoMegolmOutbound(
                    room_id = roomId.value,
                    blob = outboundGroupSession.value,
                    utcEpochMillis = creationTimestampUtc,
                )
            )
        }
    }

    suspend fun persistSession(identity: Curve25519, sessionId: SessionId, olmSession: SerializedObject) {
        withContext(dispatchers.io) {
            database.cryptoQueries.insertOlmSession(
                identity_key = identity.value,
                session_id = sessionId.value,
                blob = olmSession.value,
            )
        }
    }

    suspend fun readSessions(identities: List<Curve25519>): List<Pair<Curve25519, String>>? {
        return withContext(dispatchers.io) {
            database.cryptoQueries
                .selectOlmSession(identities.map { it.value })
                .executeAsList()
                .map { Curve25519(it.identity_key) to it.blob }
                .takeIf { it.isNotEmpty() }
        }
    }

    suspend fun startTransaction(action: suspend TransactionWithoutReturn.() -> Unit) {
        val scope = CoroutineScope(dispatchers.io)
        database.cryptoQueries.transaction {
            scope.launch { action() }
        }
    }

    suspend fun persist(sessionId: SessionId, inboundGroupSession: SerializedObject) {
        withContext(dispatchers.io) {
            database.cryptoQueries.insertMegolmInbound(
                DbCryptoMegolmInbound(
                    session_id = sessionId.value,
                    blob = inboundGroupSession.value
                )
            )
        }
    }

    suspend fun readInbound(sessionId: SessionId): SerializedObject? {
        return withContext(dispatchers.io) {
            database.cryptoQueries
                .selectMegolmInbound(sessionId.value)
                .executeAsOneOrNull()
                ?.let { SerializedObject((it)) }
        }
    }

}

@JvmInline
value class SerializedObject(val value: String)