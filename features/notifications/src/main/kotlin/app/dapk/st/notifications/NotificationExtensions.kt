package app.dapk.st.notifications

import android.app.Notification
import android.content.LocusId
import app.dapk.st.core.DeviceMeta
import app.dapk.st.core.onAtLeastQ

interface NotificationExtensions {
    fun Notification.Builder.applyLocusId(id: String)
}

internal class DefaultNotificationExtensions(private val deviceMeta: DeviceMeta) : NotificationExtensions {
    override fun Notification.Builder.applyLocusId(id: String) {
        deviceMeta.onAtLeastQ { setLocusId(LocusId(id)) }
    }
}
