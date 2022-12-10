package app.dapk.st.impl

import app.dapk.engine.core.Base64

internal class AndroidBase64 : Base64 {
    override fun encode(input: ByteArray): String {
        return android.util.Base64.encodeToString(input, android.util.Base64.DEFAULT)
    }

    override fun decode(input: String): ByteArray {
        return android.util.Base64.decode(input, android.util.Base64.DEFAULT)
    }
}