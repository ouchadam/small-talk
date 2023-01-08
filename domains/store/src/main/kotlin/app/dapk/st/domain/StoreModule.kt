package app.dapk.st.domain

import app.dapk.db.app.StDb
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.Preferences
import app.dapk.st.domain.application.eventlog.EventLogPersistence
import app.dapk.st.domain.application.eventlog.LoggingStore
import app.dapk.st.domain.application.message.MessageOptionsStore
import app.dapk.st.domain.preference.CachingPreferences
import app.dapk.st.domain.preference.PropertyCache
import app.dapk.st.domain.push.PushTokenRegistrarPreferences

class StoreModule(
    private val database: StDb,
    private val databaseDropper: DatabaseDropper,
    val preferences: Preferences,
    val credentialPreferences: Preferences,
    private val coroutineDispatchers: CoroutineDispatchers,
) {

    private val cache = PropertyCache()
    val cachingPreferences = CachingPreferences(cache, preferences)

    fun pushStore() = PushTokenRegistrarPreferences(preferences)

    fun applicationStore() = ApplicationPreferences(preferences)

    fun cacheCleaner() = StoreCleaner { cleanCredentials ->
        if (cleanCredentials) {
            credentialPreferences.clear()
        }
        preferences.clear()
        databaseDropper.dropAllTables(includeCryptoAccount = cleanCredentials)
    }

    fun eventLogStore(): EventLogPersistence {
        return EventLogPersistence(database, coroutineDispatchers)
    }

    fun loggingStore(): LoggingStore = LoggingStore(cachingPreferences)

    fun messageStore(): MessageOptionsStore = MessageOptionsStore(cachingPreferences)

}
