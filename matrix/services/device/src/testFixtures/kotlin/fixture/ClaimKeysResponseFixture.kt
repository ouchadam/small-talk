package fixture

import app.dapk.st.matrix.common.DeviceId
import app.dapk.st.matrix.common.UserId
import app.dapk.st.matrix.device.internal.ClaimKeysResponse
import kotlinx.serialization.json.JsonElement

fun aClaimKeysResponse(
    oneTimeKeys: Map<UserId, Map<DeviceId, JsonElement>> = emptyMap(),
    failures: Map<String, JsonElement> = emptyMap()
) = ClaimKeysResponse(oneTimeKeys, failures)