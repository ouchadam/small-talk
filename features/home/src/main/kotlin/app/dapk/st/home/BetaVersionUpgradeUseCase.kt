package app.dapk.st.home

import app.dapk.st.core.BuildMeta
import app.dapk.st.domain.ApplicationPreferences
import app.dapk.st.domain.ApplicationVersion
import kotlinx.coroutines.runBlocking

private const val FIRST_VERSION_TO_START_COUNTING_FROM = 3

class BetaVersionUpgradeUseCase(
    private val applicationPreferences: ApplicationPreferences,
    private val buildMeta: BuildMeta,
) {

    fun hasVersionChanged(): Boolean {
        return runBlocking {
            val previousVersion = applicationPreferences.readVersion()?.value
            val currentVersion = buildMeta.versionCode
            when (previousVersion) {
                null -> currentVersion == FIRST_VERSION_TO_START_COUNTING_FROM
                else -> currentVersion > previousVersion
            }.also {
                applicationPreferences.setVersion(ApplicationVersion(currentVersion))
            }
        }
    }

}