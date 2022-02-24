package app.dapk.st.domain

fun interface DatabaseDropper {
    suspend fun dropAllTables(includeCryptoAccount: Boolean)
}