package app.dapk.st.push

import android.content.Context
import app.dapk.st.domain.push.PushTokenRegistrarPreferences
import app.dapk.st.push.messaging.MessagingPushTokenRegistrar
import app.dapk.st.push.unifiedpush.UnifiedPush
import app.dapk.st.push.unifiedpush.UnifiedPushRegistrar

private val FIREBASE_OPTION = Registrar("Google - Firebase (FCM)")
private val NONE = Registrar("None")

class PushTokenRegistrars(
    private val context: Context,
    private val messagingPushTokenRegistrar: MessagingPushTokenRegistrar,
    private val unifiedPushRegistrar: UnifiedPushRegistrar,
    private val pushTokenStore: PushTokenRegistrarPreferences,
    private val unifiedPush: UnifiedPush,
) : PushTokenRegistrar {

    private var selection: Registrar? = null

    fun options(): List<Registrar> {
        val messagingOption = when (messagingPushTokenRegistrar.isAvailable()) {
            true -> FIREBASE_OPTION
            else -> null
        }
        return listOfNotNull(NONE, messagingOption) + unifiedPush.getDistributors(context).map { Registrar(it) }
    }

    suspend fun currentSelection() = selection ?: (pushTokenStore.currentSelection()?.let { Registrar(it) } ?: defaultSelection()).also { selection = it }

    private fun defaultSelection() = when (messagingPushTokenRegistrar.isAvailable()) {
        true -> FIREBASE_OPTION
        else -> NONE
    }

    suspend fun makeSelection(option: Registrar) {
        selection = option
        pushTokenStore.store(option.id)
        when (option) {
            NONE -> {
                messagingPushTokenRegistrar.unregister()
                unifiedPushRegistrar.unregister()
            }

            FIREBASE_OPTION -> {
                unifiedPushRegistrar.unregister()
                messagingPushTokenRegistrar.registerCurrentToken()
            }

            else -> {
                messagingPushTokenRegistrar.unregister()
                unifiedPushRegistrar.registerSelection(option)
            }
        }
    }

    override suspend fun registerCurrentToken() {
        when (currentSelection()) {
            FIREBASE_OPTION -> messagingPushTokenRegistrar.registerCurrentToken()
            NONE -> {
                // do nothing
            }

            else -> unifiedPushRegistrar.registerCurrentToken()
        }
    }

    override fun unregister() {
        when (selection) {
            FIREBASE_OPTION -> messagingPushTokenRegistrar.unregister()
            NONE -> {
                runCatching {
                    messagingPushTokenRegistrar.unregister()
                    unifiedPushRegistrar.unregister()
                }
            }

            null -> {
                // do nothing
            }

            else -> unifiedPushRegistrar.unregister()
        }
    }

}

@JvmInline
value class Registrar(val id: String)