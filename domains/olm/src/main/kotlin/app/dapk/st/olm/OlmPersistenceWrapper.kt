package app.dapk.st.olm

import app.dapk.st.core.Base64
import app.dapk.st.domain.OlmPersistence
import app.dapk.st.domain.SerializedObject
import app.dapk.st.matrix.common.Curve25519
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SessionId
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmInboundGroupSession
import org.matrix.olm.OlmOutboundGroupSession
import org.matrix.olm.OlmSession
import java.io.*

class OlmPersistenceWrapper(
    private val olmPersistence: OlmPersistence,
    private val base64: Base64,
) : OlmStore {

    override suspend fun read(): OlmAccount? {
        return olmPersistence.read()?.deserialize()
    }

    override suspend fun persist(olmAccount: OlmAccount) {
        olmPersistence.persist(SerializedObject(olmAccount.serialize()))
    }

    override suspend fun readOutbound(roomId: RoomId): Pair<Long, OlmOutboundGroupSession>? {
        return olmPersistence.readOutbound(roomId)?.let {
            it.first to it.second.deserialize()
        }
    }

    override suspend fun persistOutbound(roomId: RoomId, creationTimestampUtc: Long, outboundGroupSession: OlmOutboundGroupSession) {
        olmPersistence.persistOutbound(roomId, creationTimestampUtc, SerializedObject(outboundGroupSession.serialize()))
    }

    override suspend fun persistSession(identity: Curve25519, sessionId: SessionId, olmSession: OlmSession) {
        olmPersistence.persistSession(identity, sessionId, SerializedObject(olmSession.serialize()))
    }

    override suspend fun readSessions(identities: List<Curve25519>): List<Pair<Curve25519, OlmSession>>? {
        return olmPersistence.readSessions(identities)?.map { it.first to it.second.deserialize() }
    }

    override suspend fun persist(sessionId: SessionId, inboundGroupSession: OlmInboundGroupSession) {
        olmPersistence.persist(sessionId, SerializedObject(inboundGroupSession.serialize()))
    }

    override suspend fun readInbound(sessionId: SessionId): OlmInboundGroupSession? {
        return olmPersistence.readInbound(sessionId)?.value?.deserialize()
    }

    private fun <T : Serializable> T.serialize(): String {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use {
            it.writeObject(this)
        }
        return base64.encode(baos.toByteArray())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> String.deserialize(): T {
        val decoded = base64.decode(this)
        val baos = ByteArrayInputStream(decoded)
        return ObjectInputStream(baos).use {
            it.readObject() as T
        }
    }
}
