package app.dapk.st.push

import android.content.Context
import app.dapk.st.domain.push.PushTokenRegistrarPreferences
import app.dapk.st.push.firebase.FirebasePushTokenRegistrar
import app.dapk.st.push.unifiedpush.UnifiedPushRegistrar
import org.unifiedpush.android.connector.UnifiedPush

private val FIREBASE_OPTION = Registrar("Google - Firebase (FCM)")
private val NONE = Registrar("None")

class PushTokenRegistrars(
    private val context: Context,
    private val firebasePushTokenRegistrar: FirebasePushTokenRegistrar,
    private val unifiedPushRegistrar: UnifiedPushRegistrar,
    private val pushTokenStore: PushTokenRegistrarPreferences,
) : PushTokenRegistrar {

    private var selection: Registrar? = null

    fun options(): List<Registrar> {
        return listOf(NONE, FIREBASE_OPTION) + UnifiedPush.getDistributors(context).map { Registrar(it) }
    }

    suspend fun currentSelection() = selection ?: (pushTokenStore.currentSelection()?.let { Registrar(it) } ?: FIREBASE_OPTION).also { selection = it }

    suspend fun makeSelection(option: Registrar) {
        selection = option
        pushTokenStore.store(option.id)
        when (option) {
            NONE -> {
                firebasePushTokenRegistrar.unregister()
                unifiedPushRegistrar.unregister()
            }

            FIREBASE_OPTION -> {
                unifiedPushRegistrar.unregister()
                firebasePushTokenRegistrar.registerCurrentToken()
            }

            else -> {
                firebasePushTokenRegistrar.unregister()
                unifiedPushRegistrar.registerSelection(option)
            }
        }
    }

    override suspend fun registerCurrentToken() {
        when (selection) {
            FIREBASE_OPTION -> firebasePushTokenRegistrar.registerCurrentToken()
            NONE -> {
                // do nothing
            }

            else -> unifiedPushRegistrar.registerCurrentToken()
        }
    }

    override fun unregister() {
        when (selection) {
            FIREBASE_OPTION -> firebasePushTokenRegistrar.unregister()
            NONE -> {
                runCatching {
                    firebasePushTokenRegistrar.unregister()
                    unifiedPushRegistrar.unregister()
                }
            }

            else -> unifiedPushRegistrar.unregister()
        }
    }

}

@JvmInline
value class Registrar(val id: String)