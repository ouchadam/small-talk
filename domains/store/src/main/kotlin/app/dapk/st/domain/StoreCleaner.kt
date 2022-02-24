package app.dapk.st.domain

fun interface StoreCleaner {
    suspend fun cleanCache(removeCredentials: Boolean)
}