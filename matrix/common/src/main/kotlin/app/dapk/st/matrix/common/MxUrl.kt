package app.dapk.st.matrix.common

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
@JvmInline
value class MxUrl(val value: String)

fun MxUrl.convertMxUrToUrl(homeServer: HomeServerUrl): String {
    val mxcUri = URI.create(this.value)
    return "${homeServer.value.ensureHttps().ensureEndsWith("/")}_matrix/media/r0/download/${mxcUri.authority}${mxcUri.path}"
}

private fun String.ensureEndsWith(suffix: String) = if (endsWith(suffix)) this else "$this$suffix"

private fun String.ensureHttps() = replace("http://", "https://").ensureStartsWith("https://")

private fun String.ensureStartsWith(prefix: String) = if (startsWith(prefix)) this else "$prefix$this"
