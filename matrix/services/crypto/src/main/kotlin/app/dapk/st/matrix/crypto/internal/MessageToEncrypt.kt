package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.common.RoomId

data class MessageToEncrypt(val roomId: RoomId, val json: JsonString)