package fake

import app.dapk.st.domain.StoreCleaner
import io.mockk.mockk

class FakeStoreCleaner : StoreCleaner by mockk()
