package app.dapk.st.engine

import app.dapk.st.core.AppLogTag
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.JobBag
import app.dapk.st.core.log
import app.dapk.st.matrix.common.CredentialsStore
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.JsonString
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.message.BackgroundScheduler
import app.dapk.st.matrix.sync.RoomStore
import app.dapk.st.matrix.sync.SyncService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MatrixPushHandler(
    private val backgroundScheduler: BackgroundScheduler,
    private val credentialsStore: CredentialsStore,
    private val syncService: SyncService,
    private val roomStore: RoomStore,
    private val dispatchers: CoroutineDispatchers,
    private val jobBag: JobBag,
) : PushHandler {

    override fun onNewToken(payload: JsonString) {
        log(AppLogTag.PUSH, "new push token received")
        backgroundScheduler.schedule(
            key = "2",
            task = BackgroundScheduler.Task(
                type = "push_token",
                jsonPayload = payload
            )
        )
    }

    override fun onMessageReceived(eventId: EventId?, roomId: RoomId?) {
        log(AppLogTag.PUSH, "push received")
        jobBag.replace(MatrixPushHandler::class, dispatchers.global.launch {
            when (credentialsStore.credentials()) {
                null -> log(AppLogTag.PUSH, "push ignored due to missing api credentials")
                else -> doSync(roomId, eventId)
            }
        })
    }

    private suspend fun doSync(roomId: RoomId?, eventId: EventId?) {
        when (roomId) {
            null -> {
                log(AppLogTag.PUSH, "empty push payload - keeping sync alive until unread changes")
                waitForUnreadChange(60_000) ?: log(AppLogTag.PUSH, "timed out waiting for sync")
            }

            else -> {
                log(AppLogTag.PUSH, "push with eventId payload - keeping sync alive until the event shows up in the sync response")
                waitForEvent(
                    timeout = 60_000,
                    eventId!!,
                ) ?: log(AppLogTag.PUSH, "timed out waiting for sync")
            }
        }
        log(AppLogTag.PUSH, "push sync finished")
    }

    private suspend fun waitForEvent(timeout: Long, eventId: EventId): EventId? {
        return withTimeoutOrNull(timeout) {
            combine(syncService.startSyncing(), syncService.observeEvent(eventId)) { _, event -> event }
                .firstOrNull {
                    it == eventId
                }
        }
    }

    private suspend fun waitForUnreadChange(timeout: Long): String? {
        return withTimeoutOrNull(timeout) {
            combine(syncService.startSyncing(), roomStore.observeUnread()) { _, unread -> unread }
                .first()
            "ignored"
        }
    }
}