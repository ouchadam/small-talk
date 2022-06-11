package app.dapk.st.notifications

import fake.FakeInboxStyle
import fake.FakeMessagingStyle
import fake.FakePersonBuilder
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class AndroidNotificationStyleBuilderTest {

    private val fakePersonBuilder = FakePersonBuilder()
    private val fakeInbox = FakeInboxStyle().also { it.captureInteractions() }
    private val fakeMessagingStyle = FakeMessagingStyle()

    private val styleBuilder = AndroidNotificationStyleBuilder(
        personBuilderFactory = { fakePersonBuilder.instance },
        inboxStyleFactory = { fakeInbox.instance },
        messagingStyleFactory = {
            fakeMessagingStyle.user = it
            fakeMessagingStyle.instance
        },
    )

    @Test
    fun `given an inbox style, when building android style, then returns framework version`() {
        val input = AndroidNotificationStyle.Inbox(
            lines = listOf("hello", "world"),
            summary = "a summary"
        )

        val result = styleBuilder.build(input)

        result shouldBeEqualTo fakeInbox.instance
        fakeInbox.lines shouldBeEqualTo input.lines
        fakeInbox.summary shouldBeEqualTo input.summary
    }

}
