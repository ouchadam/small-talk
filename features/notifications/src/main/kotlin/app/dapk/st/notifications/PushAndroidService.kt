package app.dapk.st.notifications

import android.content.Context
import app.dapk.st.core.AppLogTag.PUSH
import app.dapk.st.core.extensions.unsafeLazy
import app.dapk.st.core.log
import app.dapk.st.core.module
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.work.WorkScheduler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private var previousJob: Job? = null

@OptIn(DelicateCoroutinesApi::class)
class PushAndroidService : FirebaseMessagingService() {

    private val module by unsafeLazy { module<NotificationsModule>() }
    private lateinit var context: Context

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    override fun onNewToken(token: String) {
        log(PUSH, "new push token received")
        module.workScheduler().schedule(
            WorkScheduler.WorkTask(
                type = "push_token",
                jobId = 2,
                jsonPayload = token
            )
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val eventId = message.data["event_id"]?.let { EventId(it) }
        val roomId = message.data["room_id"]?.let { RoomId(it) }

        log(PUSH, "push received")
        previousJob?.cancel()
        previousJob = GlobalScope.launch {
            when (module.credentialProvider().credentials()) {
                null -> log(PUSH, "push ignored due to missing api credentials")
                else -> doSync(roomId, eventId)
            }
        }
    }

    private suspend fun doSync(roomId: RoomId?, eventId: EventId?) {
        when (roomId) {
            null -> {
                log(PUSH, "empty push payload - keeping sync alive until unread changes")
                waitForUnreadChange(60_000) ?: log(PUSH, "timed out waiting for sync")
            }
            else -> {
                log(PUSH, "push with eventId payload - keeping sync alive until the event shows up in the sync response")
                waitForEvent(
                    timeout = 60_000,
                    eventId!!,
                ) ?: log(PUSH, "timed out waiting for sync")
            }
        }
        log(PUSH, "push sync finished")
    }

    private suspend fun waitForEvent(timeout: Long, eventId: EventId): EventId? {
        return withTimeoutOrNull(timeout) {
            combine(module.syncService().startSyncing().startInstantly(), module.syncService().observeEvent(eventId)) { _, event -> event }
                .firstOrNull {
                    it == eventId
                }
        }
    }

    private suspend fun waitForUnreadChange(timeout: Long): String? {
        return withTimeoutOrNull(timeout) {
            combine(module.syncService().startSyncing().startInstantly(), module.roomStore().observeUnread()) { _, unread -> unread }
                .first()
            "ignored"
        }
    }

}

private fun Flow<Unit>.startInstantly() = this.onStart { emit(Unit) }