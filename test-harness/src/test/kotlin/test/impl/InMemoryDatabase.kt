package test.impl

import app.dapk.db.DapkDb
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.io.File

object InMemoryDatabase {

    fun realInstance(id: String): DapkDb {
        val dbDir = "build/smalltalk-test-persistence"
        val dbPath = "$dbDir/test-$id.db"
        return DapkDb(JdbcSqliteDriver(
            url = "jdbc:sqlite:${File(dbPath).absolutePath}",
        ).also {
            if (!File(dbPath).exists()) {
                File(dbDir).mkdirs()
                DapkDb.Schema.create(it)
            }
        })
    }

}