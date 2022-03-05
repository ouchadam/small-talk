package fixture

import app.dapk.st.matrix.common.DecryptionResult
import app.dapk.st.matrix.common.JsonString

fun aDecryptionSuccessResult(
    payload: JsonString = aJsonString(),
    isVerified: Boolean = false,
) = DecryptionResult.Success(payload, isVerified)

