package app.dapk.st.push

interface PushTokenRegistrar {
    suspend fun registerCurrentToken()
    fun unregister()
}
