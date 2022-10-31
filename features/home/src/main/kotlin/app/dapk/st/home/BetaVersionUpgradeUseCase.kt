package app.dapk.st.home

import app.dapk.st.core.BuildMeta
import app.dapk.st.domain.ApplicationPreferences
import app.dapk.st.domain.ApplicationVersion
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BetaVersionUpgradeUseCase(
    private val applicationPreferences: ApplicationPreferences,
    private val buildMeta: BuildMeta,
) {

    private var _continuation: CancellableContinuation<Unit>? = null

    fun hasVersionChanged(): Boolean {
        return runBlocking { hasChangedVersion() }
    }

    private suspend fun hasChangedVersion(): Boolean {
        val previousVersion = applicationPreferences.readVersion()?.value
        val currentVersion = buildMeta.versionCode
        return when (previousVersion) {
            null -> false
            else -> currentVersion > previousVersion
        }.also {
            applicationPreferences.setVersion(ApplicationVersion(currentVersion))
        }
    }

    suspend fun waitUnitReady() {
        if (hasChangedVersion()) {
            suspendCancellableCoroutine { continuation ->
                _continuation = continuation
            }
        }
    }

    fun notifyUpgraded() {
        _continuation?.resume(Unit)
        _continuation = null
    }

}