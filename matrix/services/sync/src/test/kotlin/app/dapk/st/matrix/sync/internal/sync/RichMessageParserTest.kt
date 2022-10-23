package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RichText.Part.Link
import app.dapk.st.matrix.common.RichText.Part.Normal
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Ignore
import org.junit.Test

class RichMessageParserTest {

    private val parser = RichMessageParser()

    @Test
    fun `parses plain text`() = runParserTest(
        input = "Hello world!",
        expected = RichText(setOf(Normal("Hello world!")))
    )

    @Test
    fun `skips p tags`() = runParserTest(
        input = "Hello world! <p>foo bar</p> after paragraph",
        expected = RichText(setOf(Normal("Hello world! "), Normal("foo bar"), Normal(" after paragraph")))
    )

    @Test
    fun `skips header tags`() = runParserTest(
        Case(
            input = "<h1>hello</h1>",
            expected = RichText(setOf(Normal("hello")))
        ),
        Case(
            input = "<h2>hello</h2>",
            expected = RichText(setOf(Normal("hello")))
        ),
        Case(
            input = "<h3>hello</h3>",
            expected = RichText(setOf(Normal("hello")))
        ),
    )

    @Test
    fun `replaces br tags`() = runParserTest(
        input = "Hello world!<br />next line<br />another line",
        expected = RichText(setOf(Normal("Hello world!"), Normal("\n"), Normal("next line"), Normal("\n"), Normal("another line")))
    )

    @Test
    fun `parses urls`() = runParserTest(
        Case(
            input = "https://google.com",
            expected = RichText(setOf(Link("https://google.com", "https://google.com")))
        ),
        Case(
            input = "https://google.com. after link",
            expected = RichText(setOf(Link("https://google.com", "https://google.com"), Normal(". after link")))
        ),
        Case(
            input = "ending sentence with url https://google.com.",
            expected = RichText(setOf(Normal("ending sentence with url "), Link("https://google.com", "https://google.com"), Normal(".")))
        ),
    )

    @Test
    fun `parses styling text`() = runParserTest(
        input = "<em>hello</em> <strong>world</strong>",
        expected = RichText(setOf(RichText.Part.Italic("hello"), Normal(" "), RichText.Part.Bold("world")))
    )

    @Test
    fun `parses raw reply text`() = runParserTest(
        input = "> <@a-matrix-id:domain.foo> This is a reply",
        expected = RichText(setOf(Normal("> <@a-matrix-id:domain.foo> This is a reply")))
    )

    @Test
    fun `parses strong tags`() = runParserTest(
        Case(
            input = """hello <strong>wor</strong>ld""",
            expected = RichText(
                setOf(
                    Normal("hello "),
                    RichText.Part.Bold("wor"),
                    Normal("ld"),
                )
            )
        ),
    )

    @Test
    fun `parses em tags`() = runParserTest(
        Case(
            input = """hello <em>wor</em>ld""",
            expected = RichText(
                setOf(
                    Normal("hello "),
                    RichText.Part.Italic("wor"),
                    Normal("ld"),
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
                    Normal("hello "),
                    RichText.Part.BoldItalic("wor"),
                    Normal("ld"),
                )
            )
        ),
        Case(
            input = """<a href="www.google.com"><a href="www.google.com">www.google.com<a/><a/>""",
            expected = RichText(
                setOf(
                    Link(url = "www.google.com", label = "www.google.com"),
                    Link(url = "www.bing.com", label = "www.bing.com"),
                )
            )
        )
    )

    @Test
    fun `parses 'a' tags`() = runParserTest(
        Case(
            input = """hello world <a href="www.google.com">a link!</a> more content.""",
            expected = RichText(
                setOf(
                    Normal("hello world "),
                    Link(url = "www.google.com", label = "a link!"),
                    Normal(" more content."),
                )
            )
        ),
        Case(
            input = """<a href="www.google.com">www.google.com</a><a href="www.bing.com">www.bing.com</a>""",
            expected = RichText(
                setOf(
                    Link(url = "www.google.com", label = "www.google.com"),
                    Link(url = "www.bing.com", label = "www.bing.com"),
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