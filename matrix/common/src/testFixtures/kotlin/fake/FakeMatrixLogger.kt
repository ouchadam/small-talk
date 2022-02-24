package fake

import app.dapk.st.matrix.common.MatrixLogger

class FakeMatrixLogger : MatrixLogger {
    override fun invoke(tag: String, message: String) {
        // do nothing
    }
}