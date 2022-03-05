package internalfake

import app.dapk.st.matrix.crypto.internal.MaybeCreateAndUploadOneTimeKeysUseCase
import io.mockk.mockk

class FakeMaybeCreateAndUploadOneTimeKeysUseCase : MaybeCreateAndUploadOneTimeKeysUseCase by mockk()