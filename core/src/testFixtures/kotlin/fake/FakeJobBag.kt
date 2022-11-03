package fake

import app.dapk.st.core.JobBag
import io.mockk.mockk

class FakeJobBag {
    val instance = mockk<JobBag>()
}

