package app.dapk.st.impl

import android.content.Context
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.Preferences
import app.dapk.st.core.withIoContext

internal class SharedPreferencesDelegate(
    context: Context,
    fileName: String,
    private val coroutineDispatchers: CoroutineDispatchers,
) : Preferences {

    private val preferences by lazy { context.getSharedPreferences(fileName, Context.MODE_PRIVATE) }

    override suspend fun store(key: String, value: String) {
        coroutineDispatchers.withIoContext {
            preferences.edit().putString(key, value).apply()
        }
    }

    override suspend fun readString(key: String): String? {
        return coroutineDispatchers.withIoContext {
            preferences.getString(key, null)
        }
    }

    override suspend fun remove(key: String) {
        coroutineDispatchers.withIoContext {
            preferences.edit().remove(key).apply()
        }
    }

    override suspend fun clear() {
        coroutineDispatchers.withIoContext {
            preferences.edit().clear().commit()
        }
    }
}