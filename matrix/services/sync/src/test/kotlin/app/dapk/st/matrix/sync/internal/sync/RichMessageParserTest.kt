package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Ignore
import org.junit.Test

class RichMessageParserTest {

    private val parser = RichMessageParser()

    @Test
    fun `parses plain text`() = runParserTest(
        input = "Hello world!",
        expected = RichText(setOf(RichText.Part.Normal("Hello world!")))
    )

    @Test
    fun `parses nested b tags`() = runParserTest(
        Case(
            input = """hello <b>wor<b/>ld""",
            expected = RichText(
                setOf(
                    RichText.Part.Normal("hello "),
                    RichText.Part.Bold("wor"),
                    RichText.Part.Normal("ld"),
                )
            )
        ),
    )

    @Test
    fun `parses nested i tags`() = runParserTest(
        Case(
            input = """hello <i>wor<i/>ld""",
            expected = RichText(
                setOf(
                    RichText.Part.Normal("hello "),
                    RichText.Part.Italic("wor"),
                    RichText.Part.Normal("ld"),
                )
            )
        ),
    )

    @Ignore // TODO
    @Test
    fun `parses nested tags`() = runParserTest(
        Case(
            input = """hello <b><i>wor<i/><b/>ld""",
            expected = RichText(
                setOf(
                    RichText.Part.Normal("hello "),
                    RichText.Part.BoldItalic("wor"),
                    RichText.Part.Normal("ld"),
                )
            )
        ),
        Case(
            input = """<a href="www.google.com"><a href="www.google.com">www.google.com<a/><a/>""",
            expected = RichText(
                setOf(
                    RichText.Part.Link(url = "www.google.com", label = "www.google.com"),
                    RichText.Part.Link(url = "www.bing.com", label = "www.bing.com"),
                )
            )
        )
    )

    @Test
    fun `parses 'a' tags`() = runParserTest(
        Case(
            input = """hello world <a href="www.google.com">a link!<a/> more content.""",
            expected = RichText(
                setOf(
                    RichText.Part.Normal("hello world "),
                    RichText.Part.Link(url = "www.google.com", label = "a link!"),
                    RichText.Part.Normal(" more content."),
                )
            )
        ),
        Case(
            input = """<a href="www.google.com">www.google.com<a/><a href="www.bing.com">www.bing.com<a/>""",
            expected = RichText(
                setOf(
                    RichText.Part.Link(url = "www.google.com", label = "www.google.com"),
                    RichText.Part.Link(url = "www.bing.com", label = "www.bing.com"),
                )
            )
        ),
    )

    private fun runParserTest(vararg cases: Case) {
        val errors = mutableListOf<Throwable>()
        cases.forEach {
            runCatching { runParserTest(it.input, it.expected) }.onFailure { errors.add(it) }
        }
        if (errors.isNotEmpty()) {
            throw CompositeThrowable(errors)
        }
    }

    private fun runParserTest(input: String, expected: RichText) {
        val result = parser.parse(input)

        result shouldBeEqualTo expected
    }
}

private data class Case(val input: String, val expected: RichText)

class CompositeThrowable(inner: List<Throwable>) : Throwable() {
    init {
        inner.forEach { addSuppressed(it) }
    }
}