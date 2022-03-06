package fake

import app.dapk.st.matrix.sync.SyncService
import io.mockk.mockk

class FakeSyncService : SyncService by mockk()
