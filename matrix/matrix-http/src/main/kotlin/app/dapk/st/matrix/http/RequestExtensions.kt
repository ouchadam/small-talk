package app.dapk.st.matrix.http

fun queryMap(vararg params: Pair<String, String?>): String {
    return params.filterNot { it.second == null }.joinToString(separator = "&") { (key, value) ->
        "$key=${value}"
    }
}
