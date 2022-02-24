package app.dapk.st.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun FirebaseMessaging.token() = suspendCoroutine<String> { continuation ->
    this.token.addOnCompleteListener { task ->
        when {
            task.isSuccessful -> continuation.resume(task.result!!)
            task.isCanceled -> continuation.resumeWith(Result.failure(CancelledTokenFetchingException()))
            else -> continuation.resumeWith(Result.failure(task.exception ?: UnknownTokenFetchingFailedException()))
        }
    }
}

private class CancelledTokenFetchingException : Throwable()
private class UnknownTokenFetchingFailedException : Throwable()