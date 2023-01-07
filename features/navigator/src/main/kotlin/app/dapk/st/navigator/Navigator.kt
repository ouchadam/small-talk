package app.dapk.st.navigator

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.core.app.NavUtils
import app.dapk.st.core.AndroidUri
import app.dapk.st.core.MimeType
import app.dapk.st.matrix.common.RoomId
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Navigator {

    val navigate: Dsl

    class Dsl(
        private val activity: Activity,
        private val intentFactory: IntentFactory
    ) {

        fun toHome(clearStack: Boolean = true) {
            val home = intentFactory.home(activity).apply {
                if (clearStack) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            activity.startActivity(home)
        }

        fun upToHome() {
            activity.finish()
            activity.startActivity(intentFactory.home(activity))
        }

        fun toMessenger(roomId: RoomId, attachments: List<MessageAttachment>) {
            val intent = intentFactory.messengerAttachments(activity, roomId, attachments)
            activity.startActivity(intent)
        }

        fun toFilePicker(requestCode: Int) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }
}

interface IntentFactory {

    fun notificationOpenApp(context: Context): PendingIntent
    fun notificationOpenMessage(context: Context, roomId: RoomId): PendingIntent
    fun home(context: Context): Intent
    fun messenger(context: Context, roomId: RoomId): Intent
    fun messengerShortcut(context: Context, roomId: RoomId): Intent
    fun messengerAttachments(context: Context, roomId: RoomId, attachments: List<MessageAttachment>): Intent

}

fun navigator(intentFactory: () -> IntentFactory): ReadOnlyProperty<Activity, Navigator> {
    return NavigatorDelegate(intentFactory)
}

private class NavigatorDelegate(private val intentFactory: () -> IntentFactory) : ReadOnlyProperty<Activity, Navigator> {
    private var instanceCache: Navigator? = null
    override fun getValue(thisRef: Activity, property: KProperty<*>): Navigator {
        return instanceCache ?: DefaultNavigator(thisRef, intentFactory.invoke())
    }
}

private class DefaultNavigator(activity: Activity, intentFactory: IntentFactory) : Navigator {
    override val navigate: Navigator.Dsl = Navigator.Dsl(activity, intentFactory)
}

@Parcelize
data class MessageAttachment(val uri: AndroidUri, val type: MimeType) : Parcelable {
    private companion object : Parceler<MessageAttachment> {
        override fun create(parcel: Parcel): MessageAttachment {
            val uri = AndroidUri(parcel.readString()!!)
            val type = when (parcel.readString()!!) {
                "mimetype-image" -> MimeType.Image
                else -> throw IllegalStateException()
            }
            return MessageAttachment(uri, type)
        }

        override fun MessageAttachment.write(parcel: Parcel, flags: Int) {
            parcel.writeString(uri.value)
            when (type) {
                MimeType.Image -> parcel.writeString("mimetype-image")
            }
        }
    }

}