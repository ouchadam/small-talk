package app.dapk.st.domain.preference

@Suppress("UNCHECKED_CAST")
class PropertyCache {

    private val map = mutableMapOf<String, Any>()

    fun <T> getValue(key: String): T? {
        return map[key] as? T?
    }

    fun setValue(key: String, value: Any) {
        map[key] = value
    }

}