package app.dapk.st.impl

import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.domain.DatabaseDropper
import com.squareup.sqldelight.android.AndroidSqliteDriver

class DefaultDatabaseDropper(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val driver: AndroidSqliteDriver,
) : DatabaseDropper {

    override suspend fun dropAllTables(deleteCrypto: Boolean) {
        coroutineDispatchers.withIoContext {
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type = 'table'",
                parameters = 0
            )
            cursor.use {
                while (cursor.next()) {
                    cursor.getString(0)?.let {
                        if (!deleteCrypto && it.startsWith("dbCrypto")) {
                            // skip
                        } else {
                            driver.execute(null, "DELETE FROM $it", 0)
                        }
                    }
                }
            }
        }
    }
}