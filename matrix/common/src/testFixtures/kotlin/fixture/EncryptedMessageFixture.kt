package fixture

import app.dapk.st.matrix.common.*

fun anEncryptedMegOlmV1Message(
    cipherText: CipherText = aCipherText(),
    deviceId: DeviceId = aDeviceId(),
    senderKey: String = "a-sender-key",
    sessionId: SessionId = aSessionId(),
) = EncryptedMessageContent.MegOlmV1(cipherText, deviceId, senderKey, sessionId)

fun anEncryptedOlmV1Message(
    senderId: UserId = aUserId(),
    cipherText: Map<Curve25519, EncryptedMessageContent.CipherTextInfo> = emptyMap(),
    senderKey: Curve25519 = aCurve25519(),
) = EncryptedMessageContent.OlmV1(senderId, cipherText, senderKey)

fun aCipherTextInfo(
    body: CipherText = aCipherText(),
    type: Int = 1,
) = EncryptedMessageContent.CipherTextInfo(body, type)
