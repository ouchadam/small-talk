package fake

import android.app.Notification
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

class FakeInboxStyle {
    private val _summary = slot<String>()

    val instance = mockk<Notification.InboxStyle>()
    val lines = mutableListOf<String>()
    val summary: String
        get() = _summary.captured

    fun captureInteractions() {
        every { instance.addLine(capture(lines)) } returns instance
        every { instance.setSummaryText(capture(_summary)) } returns instance
    }


}