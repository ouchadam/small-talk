package app.dapk.st.settings.eventlogger

import app.dapk.st.core.Lce
import app.dapk.st.domain.application.eventlog.LogLine

data class EventLoggerState(
    val logs: Lce<List<String>>,
    val selectedState: SelectedState?,
)

data class SelectedState(
    val selectedPage: String,
    val content: Lce<List<LogLine>>,
    val filter: String?,
)