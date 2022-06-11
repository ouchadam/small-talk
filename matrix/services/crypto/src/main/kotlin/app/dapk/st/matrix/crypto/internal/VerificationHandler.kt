package app.dapk.st.matrix.crypto.internal

import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.Verification
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.ToDevicePayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import java.util.*

internal class VerificationHandler(
    private val deviceService: DeviceService,
    private val credentialsStore: CredentialsStore,
    private val logger: MatrixLogger,
    private val jsonCanonicalizer: JsonCanonicalizer,
    private val olm: Olm,
) {

    data class VerificationTransaction(
        val userId: UserId,
        val deviceId: DeviceId,
        val transactionId: String,
    )

    val stateFlow = MutableStateFlow<Verification.State>(Verification.State.Idle)

    var verificationTransaction = VerificationTransaction(UserId(""), DeviceId(""), "")
    var sasSession: Olm.SasSession? = null
    var requesterStartPayload: ToDevicePayload.VerificationStart? = null

    suspend fun onUserVerificationAction(action: Verification.Action) {
        when (action) {
            is Verification.Action.Request -> requestVerification(action.userId, action.deviceId)
            Verification.Action.SecureAccept -> {
                stateFlow.emit(Verification.State.ReadySent)
            }
            Verification.Action.InsecureAccept -> {
                sendToDevice(ToDevicePayload.VerificationDone(verificationTransaction.transactionId))

                stateFlow.emit(Verification.State.WaitingForDoneConfirmation)
            }
            Verification.Action.AcknowledgeMatch -> {
                val credentials = credentialsStore.credentials()!!
                val mac = sasSession!!.calculateMac(
                    credentials.userId,
                    credentials.deviceId,
                    verificationTransaction.userId,
                    verificationTransaction.deviceId,
                    verificationTransaction.transactionId
                )

                sendToDevice(
                    ToDevicePayload.VerificationMac(
                        verificationTransaction.transactionId,
                        mac.keys,
                        mac.mac
                    )
                )
            }
        }
    }

    private suspend fun requestVerification(userId: UserId, deviceId: DeviceId) {
        val transactionId = UUID.randomUUID().toString()

        verificationTransaction = VerificationTransaction(userId, deviceId, transactionId)

        sendToDevice(
            ToDevicePayload.VerificationRequest(
                fromDevice = credentialsStore.credentials()!!.deviceId,
                methods = listOf("m.sas.v1"),
                transactionId = transactionId,
                timestampPosix = System.currentTimeMillis()
            )
        )
    }

    suspend fun onVerificationEvent(event: Verification.Event) {
        logger.matrixLog(MatrixLogTag.VERIFICATION, "handling event: $event")
        when (event) {
            is Verification.Event.Requested -> {
                stateFlow.emit(Verification.State.ReadySent)

                verificationTransaction = VerificationTransaction(
                    event.userId, event.deviceId, event.transactionId
                )

                sendToDevice(
                    ToDevicePayload.VerificationReady(
                        fromDevice = credentialsStore.credentials()!!.deviceId,
                        methods = listOf("m.sas.v1"),
                        event.transactionId,
                    )
                )
            }
            is Verification.Event.Ready -> {
                val startPayload = ToDevicePayload.VerificationStart(
                    fromDevice = verificationTransaction.deviceId,
                    method = event.methods.first { it == "m.sas.v1" },
                    protocols = listOf("curve25519-hkdf-sha256"),
                    hashes = listOf("sha256"),
                    codes = listOf("hkdf-hmac-sha256"),
                    short = listOf("emoji"),
                    event.transactionId,
                )
                requesterStartPayload = startPayload
                sendToDevice(startPayload)
            }
            is Verification.Event.Started -> {
                val self = credentialsStore.credentials()!!.userId.value
                val shouldSendStart = listOf(verificationTransaction.userId.value, self).minOrNull() == self


                val startPayload = ToDevicePayload.VerificationStart(
                    fromDevice = verificationTransaction.deviceId,
                    method = event.method,
                    protocols = event.protocols,
                    hashes = event.hashes,
                    codes = event.codes,
                    short = event.short,
                    event.transactionId,
                )

                val startJson = startPayload.toCanonicalJson()

                logger.matrixLog(MatrixLogTag.VERIFICATION, "startJson: $startJson")

                sasSession = olm.sasSession(credentialsStore.credentials()!!)

                val commitment = sasSession!!.generateCommitment(hash = "sha256", startJson)

                sendToDevice(
                    ToDevicePayload.VerificationAccept(
                        transactionId = event.transactionId,
                        fromDevice = credentialsStore.credentials()!!.deviceId,
                        method = event.method,
                        protocol = "curve25519-hkdf-sha256",
                        hash = "sha256",
                        code = "hkdf-hmac-sha256",
                        short = listOf("emoji", "decimal"),
                        commitment = commitment,
                    )
                )

            }

            is Verification.Event.Accepted -> {
                sasSession = olm.sasSession(credentialsStore.credentials()!!)
                sendToDevice(
                    ToDevicePayload.VerificationKey(
                        verificationTransaction.transactionId,
                        key = sasSession!!.publicKey()
                    )
                )
            }
            is Verification.Event.Key -> {
                sasSession!!.setTheirPublicKey(event.key)
                sendToDevice(
                    ToDevicePayload.VerificationKey(
                        transactionId = event.transactionId,
                        key = sasSession!!.publicKey()
                    )
                )
                stateFlow.emit(Verification.State.WaitingForMatchConfirmation)
            }
            is Verification.Event.Mac -> {
//                val credentials = credentialsStore.credentials()!!
//
//                val mac = sasSession!!.calculateMac(
//                    credentials.userId, credentials.deviceId, event.userId, verificationTransaction.deviceId, event.transactionId
//                )
//
//                sendToDevice(
//                    ToDevicePayload.VerificationMac(
//                        event.transactionId,
//                        mac.keys,
//                        mac.mac
//                    )
//                )
                // TODO verify mac?
                sendToDevice(ToDevicePayload.VerificationDone(verificationTransaction.transactionId))
                stateFlow.emit(Verification.State.Done)
            }
            is Verification.Event.Done -> {
                // TODO
            }
        }
    }

    private fun ToDevicePayload.VerificationStart.toCanonicalJson() = jsonCanonicalizer.canonicalize(
        JsonString(Json.encodeToString(ToDevicePayload.VerificationStart.serializer(), this))
    )

    private suspend fun sendToDevice(payload: ToDevicePayload.VerificationPayload) {
        logger.matrixLog(MatrixLogTag.VERIFICATION, "sending ${payload::class.java}")

        deviceService.sendToDevice(
            when (payload) {
                is ToDevicePayload.VerificationRequest -> EventType.VERIFICATION_REQUEST
                is ToDevicePayload.VerificationStart -> EventType.VERIFICATION_START
                is ToDevicePayload.VerificationDone -> EventType.VERIFICATION_DONE
                is ToDevicePayload.VerificationReady -> EventType.VERIFICATION_READY
                is ToDevicePayload.VerificationAccept -> EventType.VERIFICATION_ACCEPT
                is ToDevicePayload.VerificationMac -> EventType.VERIFICATION_MAC
                is ToDevicePayload.VerificationKey -> EventType.VERIFICATION_KEY
            },
            verificationTransaction.transactionId,
            verificationTransaction.userId,
            verificationTransaction.deviceId,
            payload as ToDevicePayload
        )
    }

}