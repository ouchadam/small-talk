package app.dapk.st.push.unifiedpush

import android.content.Context
import app.dapk.st.core.AppLogTag
import app.dapk.st.core.log
import app.dapk.st.core.module
import app.dapk.st.matrix.common.EventId
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.push.PushModule
import app.dapk.st.push.PushTokenPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

private const val FALLBACK_UNIFIED_PUSH_GATEWAY = "https://matrix.gateway.unifiedpush.org/_matrix/push/v1/notify"
private val json = Json { ignoreUnknownKeys = true }

class UnifiedPushMessageDelegate(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val pushModuleProvider: (Context) -> PushModule = { it.module() },
    private val endpointReader: suspend (URL) -> String = {
        runCatching { it.openStream().use { String(it.readBytes()) } }.getOrNull() ?: ""
    }
) {

    fun onMessage(context: Context, message: ByteArray) {
        log(AppLogTag.PUSH, "UnifiedPush onMessage, $message")
        val module = pushModuleProvider(context)
        val handler = module.pushHandler()
        scope.launch {
            withContext(module.dispatcher().io) {
                val payload = json.decodeFromString(UnifiedPushMessagePayload.serializer(), String(message))
                handler.onMessageReceived(payload.notification.eventId?.let { EventId(it) }, payload.notification.roomId?.let { RoomId(it) })
            }
        }
    }

    fun onNewEndpoint(context: Context, endpoint: String) {
        log(AppLogTag.PUSH, "UnifiedPush onNewEndpoint $endpoint")
        val module = pushModuleProvider(context)
        val handler = module.pushHandler()
        scope.launch {
            withContext(module.dispatcher().io) {
                val matrixEndpoint = URL(endpoint).let { URL("${it.protocol}://${it.host}/_matrix/push/v1/notify") }
                val content = endpointReader(matrixEndpoint)
                val gatewayUrl = when {
                    content.contains("\"gateway\":\"matrix\"") -> matrixEndpoint.toString()
                    else -> FALLBACK_UNIFIED_PUSH_GATEWAY
                }
                handler.onNewToken(PushTokenPayload(token = endpoint, gatewayUrl = gatewayUrl))
            }
        }
    }

    @Serializable
    private data class UnifiedPushMessagePayload(
        @SerialName("notification") val notification: Notification,
    ) {

        @Serializable
        data class Notification(
            @SerialName("event_id") val eventId: String? = null,
            @SerialName("room_id") val roomId: String? = null,
        )
    }

}