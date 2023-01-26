package app.dapk.st.domain.application

import app.dapk.db.app.StDb
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.RoomId
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val DEFAULT_STATE = AppRoomState(isChatBubble = false)

class AppRoomPersistence(
    private val database: StDb,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    fun observe(roomId: RoomId): Flow<AppRoomState> {
        return database.appRoomQueries.select(roomId.value)
            .asFlow()
            .mapToOneOrNull(coroutineDispatchers.io)
            .map { it?.let { AppRoomState(isChatBubble = it.is_bubble == 1) } ?: DEFAULT_STATE }
    }

    suspend fun markBubble(roomId: RoomId) = coroutineDispatchers.withIoContext {
        database.appRoomQueries.updateBubble(roomId.value, is_bubble = 1)
    }

    suspend fun unmarkBubble(roomId: RoomId) = coroutineDispatchers.withIoContext {
        database.appRoomQueries.updateBubble(roomId.value, is_bubble = 0)
    }
}


data class AppRoomState(
    val isChatBubble: Boolean
)