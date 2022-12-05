package app.dapk.st.domain.application.eventlog

import app.dapk.db.app.StDb
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventLogPersistence(
    private val database: StDb,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    suspend fun insert(tag: String, content: String) {
        coroutineDispatchers.withIoContext {
            database.eventLoggerQueries.insert(
                tag = tag,
                content = content,
            )
        }
    }

    suspend fun days(): List<String> {
        return coroutineDispatchers.withIoContext {
            database.eventLoggerQueries.selectDays().executeAsList()
        }
    }

    fun latest(logKey: String, filter: String?): Flow<List<LogLine>> {
        return when (filter) {
            null -> database.eventLoggerQueries.selectLatestByLog(logKey)
                .asFlow()
                .mapToList(context = coroutineDispatchers.io)
                .map {
                    it.map {
                        LogLine(
                            tag = it.tag,
                            content = it.content,
                            time = it.time,
                        )
                    }
                }

            else -> database.eventLoggerQueries.selectLatestByLogFiltered(logKey, filter)
                .asFlow()
                .mapToList(context = coroutineDispatchers.io)
                .map {
                    it.map {
                        LogLine(
                            tag = it.tag,
                            content = it.content,
                            time = it.time,
                        )
                    }
                }
        }
    }

}

data class LogLine(val tag: String, val content: String, val time: String)
