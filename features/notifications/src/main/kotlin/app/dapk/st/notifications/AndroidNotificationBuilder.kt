package app.dapk.st.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.LocusId
import android.graphics.drawable.Icon
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.isAtLeastO
import app.dapk.st.core.onAtLeastO
import app.dapk.st.core.onAtLeastQ

@SuppressLint("NewApi")
private val _builderFactory: (Context, String, DeviceMeta) -> Notification.Builder = { context, channel, deviceMeta ->
    deviceMeta.isAtLeastO(
        block = { Notification.Builder(context, channel) },
        fallback = { Notification.Builder(context) }
    )
}

class AndroidNotificationBuilder(
    private val context: Context,
    private val deviceMeta: DeviceMeta,
    private val notificationStyleBuilder: AndroidNotificationStyleBuilder,
    private val builderFactory: (Context, String, DeviceMeta) -> Notification.Builder = _builderFactory,
    private val notificationExtensions: NotificationExtensions = DefaultNotificationExtensions(deviceMeta),
) {
    @SuppressLint("NewApi")
    fun build(notification: AndroidNotification): Notification {
        return builder(notification.channelId)
            .apply { setOnlyAlertOnce(!notification.alertMoreThanOnce) }
            .apply { setAutoCancel(notification.autoCancel) }
            .apply { setGroupSummary(notification.isGroupSummary) }
            .ifNotNull(notification.groupId) { setGroup(it) }
            .ifNotNull(notification.messageStyle) { style = it.build(notificationStyleBuilder) }
            .ifNotNull(notification.contentTitle) { setContentTitle(it) }
            .ifNotNull(notification.contentText) { setContentText(it) }
            .ifNotNull(notification.contentIntent) { setContentIntent(it) }
            .ifNotNull(notification.whenTimestamp) {
                setShowWhen(true)
                setWhen(it)
            }
            .ifNotNull(notification.category) { setCategory(it) }
            .ifNotNull(notification.shortcutId) {
                with(notificationExtensions) { applyLocusId(it) }
                deviceMeta.onAtLeastO { setShortcutId(it) }
            }
            .ifNotNull(notification.smallIcon) { setSmallIcon(it) }
            .ifNotNull(notification.largeIcon) { setLargeIcon(it) }
            .build()
    }

    private fun <T> Notification.Builder.ifNotNull(value: T?, action: Notification.Builder.(T) -> Unit): Notification.Builder {
        if (value != null) {
            action(value)
        }
        return this
    }

    private fun builder(channel: String) = builderFactory(context, channel, deviceMeta)
}

data class AndroidNotification(
    val channelId: String,
    val whenTimestamp: Long? = null,
    val isGroupSummary: Boolean = false,
    val groupId: String? = null,
    val groupAlertBehavior: Int? = null,
    val shortcutId: String? = null,
    val alertMoreThanOnce: Boolean,
    val contentIntent: PendingIntent? = null,
    val contentTitle: String? = null,
    val contentText: String? = null,
    val messageStyle: AndroidNotificationStyle? = null,
    val category: String? = null,
    val smallIcon: Int? = null,
    val largeIcon: Icon? = null,
    val autoCancel: Boolean = true,
) {
    fun build(builder: AndroidNotificationBuilder) = builder.build(this)
}
