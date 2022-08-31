package app.dapk.st.firebase.messaging

class Messaging {

    fun isAvailable() = false

    fun enable() {
        // do nothing
    }

    fun disable() {
        // do nothing
    }

    fun deleteToken() {
        // do nothing
    }

    suspend fun token(): String {
        return ""
    }

}