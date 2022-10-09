package app.dapk.st.matrix.crypto

import app.dapk.st.core.Base64
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.matrix.*
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.crypto.internal.*
import app.dapk.st.matrix.device.deviceService
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.net.URI

private val SERVICE_KEY = CryptoService::class

interface CryptoService : MatrixService {
    suspend fun encrypt(input: InputStream): Crypto.MediaEncryptionResult
    suspend fun encrypt(roomId: RoomId, credentials: DeviceCredentials, messageJson: JsonString): Crypto.EncryptionResult
    suspend fun decrypt(encryptedPayload: EncryptedMessageContent): DecryptionResult
    suspend fun importRoomKeys(keys: List<SharedRoomKey>)
    suspend fun InputStream.importRoomKeys(password: String): Flow<ImportResult>

    suspend fun maybeCreateMoreKeys(serverKeyCount: ServerKeyCount)
    suspend fun updateOlmSession(userIds: List<UserId>, syncToken: SyncToken?)

    suspend fun onVerificationEvent(payload: Verification.Event)
    suspend fun verificationAction(verificationAction: Verification.Action)
    fun verificationState(): Flow<Verification.State>
}

interface Crypto {

    data class EncryptionResult(
        val algorithmName: AlgorithmName,
        val senderKey: String,
        val cipherText: CipherText,
        val sessionId: SessionId,
        val deviceId: DeviceId
    )

    data class MediaEncryptionResult(
        val uri: URI,
        val contentLength: Long,
        val algorithm: String,
        val ext: Boolean,
        val keyOperations: List<String>,
        val kty: String,
        val k: String,
        val iv: String,
        val hashes: Map<String, String>,
        val v: String,
    )

}


object Verification {

    sealed interface State {
        object Idle : State
        object ReadySent : State
        object WaitingForMatchConfirmation : State
        object WaitingForDoneConfirmation : State
        object Done : State
    }

    sealed interface Event {

        data class Requested(
            val userId: UserId,
            val deviceId: DeviceId,
            val transactionId: String,
            val methods: List<String>,
            val timestamp: Long,
        ) : Event

        data class Ready(
            val userId: UserId,
            val deviceId: DeviceId,
            val transactionId: String,
            val methods: List<String>,
        ) : Event

        data class Started(
            val userId: UserId,
            val fromDevice: DeviceId,
            val method: String,
            val protocols: List<String>,
            val hashes: List<String>,
            val codes: List<String>,
            val short: List<String>,
            val transactionId: String,
        ) : Event

        data class Accepted(
            val userId: UserId,
            val fromDevice: DeviceId,
            val method: String,
            val protocol: String,
            val hash: String,
            val code: String,
            val short: List<String>,
            val transactionId: String,
        ) : Event

        data class Key(
            val userId: UserId,
            val transactionId: String,
            val key: String,
        ) : Event

        data class Mac(
            val userId: UserId,
            val transactionId: String,
            val keys: String,
            val mac: Map<String, String>,
        ) : Event

        data class Done(val transactionId: String) : Event

    }

    sealed interface Action {
        object SecureAccept : Action
        object InsecureAccept : Action
        object AcknowledgeMatch : Action
        data class Request(val userId: UserId, val deviceId: DeviceId) : Action
    }
}

fun MatrixServiceInstaller.installCryptoService(
    credentialsStore: CredentialsStore,
    olm: Olm,
    roomMembersProvider: ServiceDepFactory<RoomMembersProvider>,
    base64: Base64,
    coroutineDispatchers: CoroutineDispatchers,
): InstallExtender<CryptoService> {
    return this.install { (_, _, services, logger) ->
        val deviceService = services.deviceService()
        val accountCryptoUseCase = FetchAccountCryptoUseCaseImpl(credentialsStore, olm, deviceService)

        val registerOlmSessionUseCase = RegisterOlmSessionUseCaseImpl(olm, deviceService, logger)
        val encryptMegolmUseCase = EncryptMessageWithMegolmUseCaseImpl(
            olm,
            FetchMegolmSessionUseCaseImpl(
                olm,
                deviceService,
                accountCryptoUseCase,
                roomMembersProvider.create(services),
                registerOlmSessionUseCase,
                ShareRoomKeyUseCaseImpl(credentialsStore, deviceService, logger, olm),
                logger,
            ),
            logger,
        )

        val olmCrypto = OlmCrypto(
            olm,
            encryptMegolmUseCase,
            accountCryptoUseCase,
            UpdateKnownOlmSessionUseCaseImpl(accountCryptoUseCase, deviceService, registerOlmSessionUseCase, logger),
            MaybeCreateAndUploadOneTimeKeysUseCaseImpl(accountCryptoUseCase, olm, credentialsStore, deviceService, logger),
            logger
        )
        val verificationHandler = VerificationHandler(deviceService, credentialsStore, logger, JsonCanonicalizer(), olm)
        val roomKeyImporter = RoomKeyImporter(base64, coroutineDispatchers)
        val mediaEncrypter = MediaEncrypter(base64)

        SERVICE_KEY to DefaultCryptoService(olmCrypto, verificationHandler, roomKeyImporter, mediaEncrypter, logger)
    }
}

fun MatrixServiceProvider.cryptoService(): CryptoService = this.getService(key = SERVICE_KEY)

fun interface RoomMembersProvider {
    suspend fun userIdsForRoom(roomId: RoomId): List<UserId>
}

sealed interface ImportResult {
    data class Success(val roomIds: Set<RoomId>, val totalImportedKeysCount: Long) : ImportResult
    data class Error(val cause: Type) : ImportResult {

        sealed interface Type {
            data class Unknown(val cause: Throwable) : Type
            object NoKeysFound : Type
            object UnexpectedDecryptionOutput : Type
            object UnableToOpenFile : Type
            object InvalidFile : Type
        }

    }

    data class Update(val importedKeysCount: Long) : ImportResult
}
