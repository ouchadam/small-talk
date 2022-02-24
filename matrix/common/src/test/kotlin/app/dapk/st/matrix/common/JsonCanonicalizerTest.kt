package app.dapk.st.matrix.common

import org.amshove.kluent.ErrorCollectionMode
import org.amshove.kluent.errorCollector
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.throwCollectedErrors
import org.junit.Test

class JsonCanonicalizerTest {

    private data class Case(val input: String, val expected: String)

    private val jsonCanonicalizer = JsonCanonicalizer()

    @Test
    fun `canonicalises json strings`() {
        val cases = listOf(
            Case(
                input = """{}""",
                expected = """{}""",
            ),
            Case(
                input = """
                    {
                        "one": 1,
                        "two": "Two"
                    }
                """.trimIndent(),
                expected = """{"one":1,"two":"Two"}"""
            ),
            Case(
                input = """
                    {
                        "b": "2",
                        "a": "1"
                    }
                """.trimIndent(),
                expected = """{"a":"1","b":"2"}"""
            ),
            Case(
                input = """{"b":"2","a":"1"}""",
                expected = """{"a":"1","b":"2"}"""
            ),
            Case(
                input = """
                    {
                        "auth": {
                            "success": true,
                            "mxid": "@john.doe:example.com",
                            "profile": {
                                "display_name": "John Doe",
                                "three_pids": [
                                    {
                                        "medium": "email",
                                        "address": "john.doe@example.org"
                                    },
                                    {
                                        "medium": "msisdn",
                                        "address": "123456789"
                                    }
                                ]
                            }
                        }
                    }
                """.trimIndent(),
                expected = """{"auth":{"mxid":"@john.doe:example.com","profile":{"display_name":"John Doe","three_pids":[{"address":"john.doe@example.org","medium":"email"},{"address":"123456789","medium":"msisdn"}]},"success":true}}""",
            ),
            Case(
                input = """
                    {
                        "a": "   "
                    }
                """.trimIndent(),
                expected = """{"a":"   "}""",
            ),
            Case(
                input = """
                    {
                        "a": "\u65E5"
                    }
                """.trimIndent(),
                expected = """{"a":" "}"""
            ),
            Case(
                input = """
                    {
                        "a": null
                    }
                """.trimIndent(),
                expected = """{"a":null}"""
            )
        )

        runCases(cases) { (input, expected) ->
            val result = jsonCanonicalizer.canonicalize(JsonString(input))

            result shouldBeEqualTo expected
        }
    }
}

private inline fun <T> runCases(cases: List<T>, action: (T) -> Unit) {
    errorCollector.setCollectionMode(ErrorCollectionMode.Soft)
    cases.forEach {
        action(it)
    }
    errorCollector.throwCollectedErrors()
}