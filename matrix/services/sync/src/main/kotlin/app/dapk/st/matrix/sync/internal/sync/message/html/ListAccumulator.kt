package app.dapk.st.matrix.sync.internal.sync.message.html

import app.dapk.st.matrix.sync.internal.sync.message.ContentAccumulator

internal interface ListAccumulator {
    fun appendLinePrefix(index: Int?)
}

internal class OrderedListAccumulator(delegate: ContentAccumulator) : ContentAccumulator by delegate, ListAccumulator {

    private var currentIndex = 1

    override fun appendLinePrefix(index: Int?) {
        currentIndex = index ?: currentIndex
        appendText("$currentIndex. ")
        currentIndex++
    }
}

internal class UnorderedListAccumulator(delegate: ContentAccumulator) : ContentAccumulator by delegate, ListAccumulator {
    override fun appendLinePrefix(index: Int?) = appendText("- ")
}

