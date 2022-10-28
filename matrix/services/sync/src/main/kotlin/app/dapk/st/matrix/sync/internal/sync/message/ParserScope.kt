package app.dapk.st.matrix.sync.internal.sync.message

internal typealias SearchIndex = Int

internal fun Int.next() = this + 1


internal interface ParserScope {
    fun appendTextBeforeTag(searchIndex: Int, tagOpen: Int, builder: PartBuilder, input: String)

    fun SearchIndex.next(): SearchIndex

}