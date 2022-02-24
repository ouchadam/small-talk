package fixture

import app.dapk.st.matrix.common.DeviceCredentials
import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.UserId

fun aDeviceCredentials(
    userId: UserId = aUserId(),
    deviceId: DeviceId = aDeviceId(),
) = object : DeviceCredentials {
    override val userId = userId
    override val deviceId = deviceId
}