package app.dapk.st.core

interface Base64 {
    fun encode(input: ByteArray): String
    fun decode(input: String): ByteArray
}