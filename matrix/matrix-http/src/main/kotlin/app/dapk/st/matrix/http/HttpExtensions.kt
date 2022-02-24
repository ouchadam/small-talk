package app.dapk.st.matrix.http

fun String.ensureTrailingSlash(): String {
    return if (this.endsWith("/")) this else "$this/"
}

fun String.ensureHttps(): String {
    return if (this.startsWith("https")) this else "https://$this"
}
