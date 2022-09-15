package fixture

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.device.internal.DeviceKeys
import io.mockk.mockk

fun anAccountCryptoSession(
    fingerprint: Ed25519 = aEd25519(),
    senderKey: Curve25519 = aCurve25519(),
    deviceKeys: DeviceKeys = aDeviceKeys(),
    maxKeys: Int = 5,
    hasKeys: Boolean = false,
    olmAccount: Any = mockk(),
) = Olm.AccountCryptoSession(fingerprint, senderKey, deviceKeys, hasKeys, maxKeys, olmAccount)

fun aRoomCryptoSession(
    creationTimestampUtc: Long = 0L,
    key: String = "a-room-key",
    messageIndex: Int = 100,
    accountCryptoSession: Olm.AccountCryptoSession = anAccountCryptoSession(),
    id: SessionId = aSessionId("a-room-crypto-session-id"),
    outBound: Any = mockk(),
) = Olm.RoomCryptoSession(creationTimestampUtc, key, messageIndex, accountCryptoSession, id, outBound)

fun aDeviceCryptoSession(
    deviceId: DeviceId = aDeviceId(),
    userId: UserId = aUserId(),
    identity: Curve25519 = aCurve25519(),
    fingerprint: Ed25519 = aEd25519(),
    olmSession: List<Any> = emptyList(),
) = Olm.DeviceCryptoSession(deviceId, userId, identity, fingerprint, olmSession)
