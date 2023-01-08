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
        val readVersion = applicationPreferences.readVersion()
        val previousVersion = readVersion?.value
        val currentVersion = buildMeta.versionCode
        return when (previousVersion) {
            null -> false
            else -> currentVersion > previousVersion
        }
    }

    suspend fun waitUnitReady() {
        if (hasChangedVersion()) {
            suspendCancellableCoroutine { continuation ->
                _continuation = continuation
            }
        }
    }

    suspend fun notifyUpgraded() {
        applicationPreferences.setVersion(ApplicationVersion(buildMeta.versionCode))
        _continuation?.resume(Unit)
        _continuation = null
    }

}