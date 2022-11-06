package app.dapk.st.matrix.sync.internal.sync

import app.dapk.st.matrix.common.RichText
import app.dapk.st.matrix.common.RichText.Part.*
import app.dapk.st.matrix.sync.internal.sync.message.RichMessageParser
import fixture.aUserId
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class RichTextMessageParserTest {

    private val parser = RichMessageParser()

    @Test
    fun `parses plain text`() = runParserTest(
        input = "Hello world!",
        expected = RichText(listOf(Normal("Hello world!")))
    )

    @Test
    fun `parses strong tags`() = runParserTest(
        Case(
            input = """hello <strong>wor</strong>ld""",
            expected = RichText(
                listOf(
                    Normal("hello "),
                    Bold("wor"),
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
                listOf(
                    Normal("hello "),
                    Italic("wor"),
                    Normal("ld"),
                )
            )
        ),
    )

    @Test
    fun `parses p tags`() = runParserTest(
        input = "<p>Hello world!</p><p>foo bar</p>after paragraph",
        expected = RichText(listOf(Normal("Hello world!\nfoo bar\nafter paragraph")))
    )

    @Test
    fun `parses nesting within p tags`() = runParserTest(
        input = "<p><b>Hello world!</b></p>",
        expected = RichText(listOf(Bold("Hello world!")))
    )

    @Test
    fun `replaces quote entity`() = runParserTest(
        input = "Hello world! &quot;foo bar&quot;",
        expected = RichText(listOf(Normal("Hello world! \"foo bar\"")))
    )

    @Test
    fun `replaces ampersand entity`() = runParserTest(
        input = "Hello &amp; world!",
        expected = RichText(listOf(Normal("Hello & world!")))
    )

    @Test
    fun `replaces apostrophe entity`() = runParserTest(
        Case(
            input = "Hello world! foo&#39;s bar",
            expected = RichText(listOf(Normal("Hello world! foo's bar")))
        ),
        Case(
            input = "Hello world! foo&apos;s bar",
            expected = RichText(listOf(Normal("Hello world! foo's bar")))
        ),
    )

    @Test
    fun `replaces people`() = runParserTest(
        input = "Hello <@my-name:a-domain.foo>!",
        expected = RichText(listOf(Normal("Hello "), Person(aUserId("@my-name:a-domain.foo"), "@my-name:a-domain.foo"), Normal("!")))
    )

    @Test
    fun `replaces matrixdotto with person`() = runParserTest(
        input = """Hello <a href="https://matrix.to/#/@a-name:foo.bar">a-name</a>: world""",
        expected = RichText(listOf(Normal("Hello "), Person(aUserId("@a-name:foo.bar"), "@a-name"), Normal(": world")))
    )

    @Test
    fun `parses header tags`() = runParserTest(
        Case(
            input = "<h1>hello</h1>",
            expected = RichText(listOf(Bold("hello")))
        ),
        Case(
            input = "<h1>hello</h1>text after title",
            expected = RichText(listOf(Bold("hello"), Normal("\ntext after title")))
        ),
        Case(
            input = "<h2>hello</h2>",
            expected = RichText(listOf(Bold("hello")))
        ),
        Case(
            input = "<h3>hello</h3>",
            expected = RichText(listOf(Bold("hello")))
        ),
        Case(
            input = "<h1>1</h1>\n<h2>1</h2>\n<h3>1</h3>\n",
            expected = RichText(listOf(Bold("1"), Normal("\n\n"), Bold("1"), Normal("\n\n"), Bold("1")))
        ),
    )

    @Test
    fun `replaces br tags`() = runParserTest(
        input = "Hello world!<br />next line<br />another line",
        expected = RichText(listOf(Normal("Hello world!\nnext line\nanother line")))
    )

    @Test
    fun `parses blockquote tags`() = runParserTest(
        input = "<blockquote>\n<p><strong>hello</strong> <em>world</em></p>\n</blockquote>\n",
        expected = RichText(listOf(Normal("> "), Bold("hello"), Normal(" "), Italic("world")))
    )

    @Test
    fun `parses lists`() = runParserTest(
        Case(
            input = "<ul><li>content in list item</li><li>another item in list</li></ul>",
            expected = RichText(listOf(Normal("- content in list item\n- another item in list")))
        ),
        Case(
            input = "<ol><li>content in list item</li><li>another item in list</li></ol>",
            expected = RichText(listOf(Normal("1. content in list item\n2. another item in list")))
        ),
        Case(
            input = """<ol><li value="5">content in list item</li><li>another item in list</li></ol>""",
            expected = RichText(listOf(Normal("5. content in list item\n6. another item in list")))
        ),
        Case(
            input = """<ol><li value="3">content in list item</li><li>another item in list</li><li value="10">another change</li><li>without value</li></ol>""",
            expected = RichText(listOf(Normal("3. content in list item\n4. another item in list\n10. another change\n11. without value")))
        ),
    )


    @Test
    fun `parses nested lists`() = runParserTest(
        input = """
            <ul>
            <li>first item
                <ul>
                    <li>nested item</li>
                </ul>
            </li>
            </ul>
        """.trimIndent().lines().joinToString("") { it.trim() },
        expected = RichText(listOf(Normal("- first item\n- nested item")))
    )

    @Test
    fun `parses urls`() = runParserTest(
        Case(
            input = "https://google.com",
            expected = RichText(listOf(Link("https://google.com", "https://google.com")))
        ),
        Case(
            input = "https://google.com. after link",
            expected = RichText(listOf(Link("https://google.com", "https://google.com"), Normal(". after link")))
        ),
        Case(
            input = "ending sentence with url https://google.com.",
            expected = RichText(listOf(Normal("ending sentence with url "), Link("https://google.com", "https://google.com"), Normal(".")))
        ),
        Case(
            input = "https://google.com<br>html after url",
            expected = RichText(listOf(Link("https://google.com", "https://google.com"), Normal("\nhtml after url")))
        ),
    )

    @Test
    fun `removes reply fallback`() = runParserTest(
        input = """
            <mx-reply>
              <blockquote>
              Original message
              </blockquote>
            </mx-reply>
            Reply to message
        """.trimIndent(),
        expected = RichText(listOf(Normal("Reply to message")))
    )

    @Test
    fun `removes text fallback`() = runParserTest(
        input = """
            > <@user:domain.foo> Original message
            > Some more content
            
            Reply to message
        """.trimIndent(),
        expected = RichText(listOf(Normal("Reply to message")))
    )

    @Test
    fun `parses styling text`() = runParserTest(
        input = "<em>hello</em> <strong>world</strong>",
        expected = RichText(listOf(Italic("hello"), Normal(" "), Bold("world")))
    )

    @Test
    fun `parses invalid tags text`() = runParserTest(
        input = ">><foo> ><>> << more content",
        expected = RichText(listOf(Normal(">><foo> ><>> << more content")))
    )

    @Test
    fun `parses 'a' tags`() = runParserTest(
        Case(
            input = """hello world <a href="www.google.com">a link!</a> more content.""",
            expected = RichText(
                listOf(
                    Normal("hello world "),
                    Link(url = "www.google.com", label = "a link!"),
                    Normal(" more content."),
                )
            )
        ),
        Case(
            input = """<a href="www.google.com">www.google.com</a><a href="www.bing.com">www.bing.com</a>""",
            expected = RichText(
                listOf(
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