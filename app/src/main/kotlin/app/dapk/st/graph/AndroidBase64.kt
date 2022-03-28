package app.dapk.st.graph

import app.dapk.st.core.Base64

class AndroidBase64 : Base64 {
    override fun encode(input: ByteArray): String {
        return android.util.Base64.encodeToString(input, android.util.Base64.DEFAULT)
    }

    override fun decode(input: String): ByteArray {
        return android.util.Base64.decode(input, android.util.Base64.DEFAULT)
    }
}