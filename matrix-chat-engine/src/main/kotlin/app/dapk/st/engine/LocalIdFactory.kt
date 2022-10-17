package app.dapk.st.engine

import java.util.*

internal class LocalIdFactory {
    fun create() = "local.${UUID.randomUUID()}"
}