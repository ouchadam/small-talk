package app.dapk.st.matrix.http

fun String.ensureTrailingSlash(): String {
    return if (this.endsWith("/")) this else "$this/"
}

fun String.ensureHttpsIfMissing(): String {
    return if (this.startsWith("http")) this else "https://$this"
}
