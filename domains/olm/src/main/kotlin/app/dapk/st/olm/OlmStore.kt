package app.dapk.st.olm

import app.dapk.st.matrix.common.Curve25519
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SessionId
import org.matrix.olm.OlmAccount
import org.matrix.olm.OlmInboundGroupSession
import org.matrix.olm.OlmOutboundGroupSession
import org.matrix.olm.OlmSession

interface OlmStore {
    suspend fun read(): OlmAccount?
    suspend fun persist(olmAccount: OlmAccount)

    suspend fun transaction(action: suspend () -> Unit)
    suspend fun readOutbound(roomId: RoomId): Pair<Long, OlmOutboundGroupSession>?
    suspend fun persistOutbound(roomId: RoomId, creationTimestampUtc: Long, outboundGroupSession: OlmOutboundGroupSession)
    suspend fun persistSession(identity: Curve25519, sessionId: SessionId, olmSession: OlmSession)
    suspend fun readSessions(identities: List<Curve25519>): List<Pair<Curve25519, OlmSession>>?
    suspend fun persist(sessionId: SessionId, inboundGroupSession: OlmInboundGroupSession)
    suspend fun readInbound(sessionId: SessionId): OlmInboundGroupSession?
}