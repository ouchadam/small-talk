package fixture

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.device.internal.DeviceKeys


fun aDeviceKeys(
    userId: UserId = aUserId(),
    deviceId: DeviceId = aDeviceId(),
    algorithms: List<AlgorithmName> = listOf(anAlgorithmName()),
    keys: Map<String, String> = emptyMap(),
    signatures: Map<String, Map<String, String>> = emptyMap(),
) = DeviceKeys(userId, deviceId, algorithms, keys, signatures)