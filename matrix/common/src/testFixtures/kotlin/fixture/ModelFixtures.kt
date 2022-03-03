package fixture

import app.dapk.st.matrix.common.*

fun aUserId(value: String = "a-user-id") = UserId(value)
fun aRoomId(value: String = "a-room-id") = RoomId(value)
fun anEventId(value: String = "an-event-id") = EventId(value)
fun aDeviceId(value: String = "a-device-id") = DeviceId(value)
fun aSessionId(value: String = "a-session-id") = SessionId(value)
fun aCipherText(value: String = "cipher-content") = CipherText(value)
fun aCurve25519(value: String = "curve-value") = Curve25519(value)
fun aEd25519(value: String = "ed-value") = Ed25519(value)
fun anAlgorithmName(value: String = "an-algorithm") = AlgorithmName(value)
fun aJsonString(value: String = "{}") = JsonString(value)
fun aSyncToken(value: String = "a-sync-token") = SyncToken(value)