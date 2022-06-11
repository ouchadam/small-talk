package app.dapk.st.notifications

import android.app.Notification
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi

sealed interface AndroidNotificationStyle {

    fun build(builder: AndroidNotificationStyleBuilder): Notification.Style

    data class Inbox(val lines: List<String>, val summary: String? = null) : AndroidNotificationStyle {
        override fun build(builder: AndroidNotificationStyleBuilder) = builder.build(this)
    }

    data class Messaging(
        val person: AndroidPerson,
        val title: String?,
        val isGroup: Boolean,
        val content: List<AndroidMessage>,
    ) : AndroidNotificationStyle {

        @RequiresApi(Build.VERSION_CODES.P)
        override fun build(builder: AndroidNotificationStyleBuilder) = builder.build(this)

        data class AndroidPerson(val name: String, val key: String, val icon: Icon? = null)
        data class AndroidMessage(val sender: AndroidPerson, val content: String, val timestamp: Long)
    }

}