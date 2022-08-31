package app.dapk.st.firebase.messaging

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Messaging(
    private val instance: FirebaseMessaging,
    private val context: Context,
) {

    fun isAvailable() = GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    fun enable() {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FirebasePushServiceDelegate::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    fun disable() {
        context.stopService(Intent(context, FirebasePushServiceDelegate::class.java))
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, FirebasePushServiceDelegate::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

    }

    fun deleteToken() {
        instance.deleteToken()
    }

    suspend fun token() = suspendCoroutine { continuation ->
        instance.token.addOnCompleteListener { task ->
            when {
                task.isSuccessful -> continuation.resume(task.result!!)
                task.isCanceled -> continuation.resumeWith(Result.failure(CancelledTokenFetchingException()))
                else -> continuation.resumeWith(Result.failure(task.exception ?: UnknownTokenFetchingFailedException()))
            }
        }
    }

    private class CancelledTokenFetchingException : Throwable()
    private class UnknownTokenFetchingFailedException : Throwable()
}