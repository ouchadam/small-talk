package fixture

import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.device.DeviceService

fun aKeyClaim(
    userId: UserId = aUserId(),
    deviceId: DeviceId = aDeviceId(),
    algorithmName: AlgorithmName = anAlgorithmName(),
) = DeviceService.KeyClaim(userId, deviceId, algorithmName)