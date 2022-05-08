package app.dapk.st.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import app.dapk.st.matrix.common.RoomId
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
            activity.navigateUpTo(intentFactory.home(activity))
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

    fun home(context: Context): Intent
    fun messenger(context: Context, roomId: RoomId): Intent
    fun messengerShortcut(context: Context, roomId: RoomId): Intent

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

