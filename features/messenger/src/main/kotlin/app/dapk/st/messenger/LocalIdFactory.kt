package app.dapk.st.messenger

import java.util.*

internal class LocalIdFactory {
    fun create() = "local.${UUID.randomUUID()}"
}