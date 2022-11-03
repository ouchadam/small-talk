package app.dapk.st.push

import app.dapk.st.domain.push.PushTokenRegistrarPreferences
import app.dapk.st.push.messaging.MessagingPushTokenRegistrar
import app.dapk.st.push.unifiedpush.UnifiedPushRegistrar

private val FIREBASE_OPTION = Registrar("Google - Firebase (FCM)")
private val NONE = Registrar("None")

class PushTokenRegistrars(
    private val messagingPushTokenRegistrar: MessagingPushTokenRegistrar,
    private val unifiedPushRegistrar: UnifiedPushRegistrar,
    private val pushTokenStore: PushTokenRegistrarPreferences,
    private val state: SelectionState = SelectionState(selection = null),
) : PushTokenRegistrar {

    fun options(): List<Registrar> {
        val messagingOption = when (messagingPushTokenRegistrar.isAvailable()) {
            true -> FIREBASE_OPTION
            else -> null
        }
        return listOfNotNull(NONE, messagingOption) + unifiedPushRegistrar.getDistributors()
    }

    suspend fun currentSelection() = state.selection ?: (readStoredSelection() ?: defaultSelection()).also { state.selection = it }

    private suspend fun readStoredSelection() = pushTokenStore.currentSelection()?.let { Registrar(it) }?.takeIf { options().contains(it) }

    private fun defaultSelection() = when (messagingPushTokenRegistrar.isAvailable()) {
        true -> FIREBASE_OPTION
        else -> NONE
    }

    suspend fun makeSelection(option: Registrar) {
        state.selection = option
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
        when (state.selection) {
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

data class SelectionState(var selection: Registrar?)
