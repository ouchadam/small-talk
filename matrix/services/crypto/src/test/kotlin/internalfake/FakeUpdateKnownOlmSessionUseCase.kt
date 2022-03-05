package internalfake

import app.dapk.st.matrix.crypto.internal.UpdateKnownOlmSessionUseCase
import io.mockk.mockk

class FakeUpdateKnownOlmSessionUseCase : UpdateKnownOlmSessionUseCase by mockk()