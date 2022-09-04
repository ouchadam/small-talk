package app.dapk.st.olm

import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.core.SingletonFlows
import app.dapk.st.core.extensions.ErrorTracker
import app.dapk.st.core.extensions.ifNull
import app.dapk.st.core.withIoContext
import app.dapk.st.matrix.common.*
import app.dapk.st.matrix.common.MatrixLogTag.CRYPTO
import app.dapk.st.matrix.crypto.Olm
import app.dapk.st.matrix.crypto.Olm.*
import app.dapk.st.matrix.device.DeviceService
import app.dapk.st.matrix.device.internal.DeviceKeys
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.matrix.olm.*
import java.time.Clock

private const val SEVEN_DAYS_MILLIS = 604800000
private const val MEGOLM_ROTATION_MESSAGE_COUNT = 100
private const val INIT_OLM = "init-olm"

class OlmWrapper(
    private val olmStore: OlmStore,
    private val singletonFlows: SingletonFlows,
    private val jsonCanonicalizer: JsonCanonicalizer,
    private val deviceKeyFactory: DeviceKeyFactory,
    private val errorTracker: ErrorTracker,
    private val logger: MatrixLogger,
    private val clock: Clock,
    coroutineDispatchers: CoroutineDispatchers
) : Olm {

    init {
        coroutineDispatchers.global.launch {
            coroutineDispatchers.withIoContext {
                singletonFlows.getOrPut(INIT_OLM) {
                    OlmManager()
                }.collect()
            }
        }
    }

    override suspend fun import(keys: List<SharedRoomKey>) {
        interactWithOlm()

        olmStore.transaction {
            keys.forEach {
                val inBound = when (it.isExported) {
                    true -> OlmInboundGroupSession.importSession(it.sessionKey)
                    false -> OlmInboundGroupSession(it.sessionKey)
                }
                olmStore.persist(it.sessionId, inBound)
            }
        }
    }

    override suspend fun ensureAccountCrypto(deviceCredentials: DeviceCredentials, onCreate: suspend (AccountCryptoSession) -> Unit): AccountCryptoSession {
        interactWithOlm()
        return singletonFlows.getOrPut("account-crypto") {
            accountCrypto(deviceCredentials) ?: createAccountCrypto(deviceCredentials, onCreate)
        }.first()
    }

    private suspend fun accountCrypto(deviceCredentials: DeviceCredentials): AccountCryptoSession? {
        return olmStore.read()?.let { olmAccount ->
            createAccountCryptoSession(deviceCredentials, olmAccount)
        }
    }

    override suspend fun AccountCryptoSession.generateOneTimeKeys(
        count: Int,
        credentials: DeviceCredentials,
        publishKeys: suspend (DeviceService.OneTimeKeys) -> Unit
    ) {
        interactWithOlm()
        val olmAccount = this.olmAccount as OlmAccount
        olmAccount.generateOneTimeKeys(count)

        val oneTimeKeys = DeviceService.OneTimeKeys(olmAccount.oneTimeKeys()["curve25519"]!!.map {
            DeviceService.OneTimeKeys.Key.SignedCurve(
                keyId = it.key,
                value = it.value,
                signature = DeviceService.OneTimeKeys.Key.SignedCurve.Ed25519Signature(
                    value = it.value.toSignedJson(olmAccount),
                    deviceId = credentials.deviceId,
                    userId = credentials.userId,
                )
            )
        })
        publishKeys(oneTimeKeys)
        olmAccount.markOneTimeKeysAsPublished()
        updateAccountInstance(olmAccount)
    }

    private suspend fun createAccountCrypto(deviceCredentials: DeviceCredentials, action: suspend (AccountCryptoSession) -> Unit): AccountCryptoSession {
        val olmAccount = OlmAccount()
        return createAccountCryptoSession(deviceCredentials, olmAccount).also {
            action(it)
            olmStore.persist(olmAccount)
        }
    }

    private fun createAccountCryptoSession(credentials: DeviceCredentials, olmAccount: OlmAccount): AccountCryptoSession {
        val (identityKey, senderKey) = olmAccount.readIdentityKeys()
        return AccountCryptoSession(
            fingerprint = identityKey,
            senderKey = senderKey,
            deviceKeys = deviceKeyFactory.create(credentials.userId, credentials.deviceId, identityKey, senderKey, olmAccount),
            olmAccount = olmAccount,
            maxKeys = olmAccount.maxOneTimeKeys().toInt()
        )
    }

    override suspend fun ensureRoomCrypto(
        roomId: RoomId,
        accountSession: AccountCryptoSession,
    ): RoomCryptoSession {
        interactWithOlm()
        return singletonFlows.getOrPut("room-${roomId.value}") {
            roomCrypto(roomId, accountSession) ?: createRoomCrypto(roomId, accountSession)
        }
            .first()
            .maybeRotateRoomSession(roomId, accountSession)
    }

    private suspend fun RoomCryptoSession.maybeRotateRoomSession(roomId: RoomId, accountSession: AccountCryptoSession): RoomCryptoSession {
        val now = clock.millis()
        return when {
            this.messageIndex > MEGOLM_ROTATION_MESSAGE_COUNT || (now - this.creationTimestampUtc) > SEVEN_DAYS_MILLIS -> {
                logger.matrixLog(CRYPTO, "rotating megolm for room ${roomId.value}")
                createRoomCrypto(roomId, accountSession).also { rotatedSession ->
                    singletonFlows.update("room-${roomId.value}", rotatedSession)
                }
            }
            else -> this
        }
    }

    private suspend fun roomCrypto(roomId: RoomId, accountCryptoSession: AccountCryptoSession): RoomCryptoSession? {
        return olmStore.readOutbound(roomId)?.let { (timestampUtc, outBound) ->
            RoomCryptoSession(
                creationTimestampUtc = timestampUtc,
                key = outBound.sessionKey(),
                messageIndex = outBound.messageIndex(),
                accountCryptoSession = accountCryptoSession,
                id = SessionId(outBound.sessionIdentifier()),
                outBound = outBound
            )
        }
    }

    private suspend fun createRoomCrypto(roomId: RoomId, accountCryptoSession: AccountCryptoSession): RoomCryptoSession {
        val outBound = OlmOutboundGroupSession()
        val roomCryptoSession = RoomCryptoSession(
            creationTimestampUtc = clock.millis(),
            key = outBound.sessionKey(),
            messageIndex = outBound.messageIndex(),
            accountCryptoSession = accountCryptoSession,
            id = SessionId(outBound.sessionIdentifier()),
            outBound = outBound
        )
        olmStore.persistOutbound(roomId, roomCryptoSession.creationTimestampUtc, outBound)

        val inBound = OlmInboundGroupSession(roomCryptoSession.key)
        olmStore.persist(roomCryptoSession.id, inBound)

        return roomCryptoSession
    }

    override suspend fun ensureDeviceCrypto(input: OlmSessionInput, olmAccount: AccountCryptoSession): DeviceCryptoSession {
        interactWithOlm()
        return deviceCrypto(input) ?: createDeviceCrypto(olmAccount, input)
    }

    private suspend fun deviceCrypto(input: OlmSessionInput): DeviceCryptoSession? {
        return olmStore.readSessions(listOf(input.identity))?.let {
            DeviceCryptoSession(
                input.deviceId, input.userId, input.identity, input.fingerprint, it
            )
        }
    }

    private suspend fun createDeviceCrypto(accountCryptoSession: AccountCryptoSession, input: OlmSessionInput): DeviceCryptoSession {
        val olmSession = OlmSession()
        olmSession.initOutboundSession(accountCryptoSession.olmAccount as OlmAccount, input.identity.value, input.oneTimeKey)
        val sessionId = SessionId(olmSession.sessionIdentifier())
        logger.crypto("creating olm session: $sessionId ${input.identity} ${input.userId} ${input.deviceId}")
        olmStore.persistSession(input.identity, sessionId, olmSession)
        return DeviceCryptoSession(input.deviceId, input.userId, input.identity, input.fingerprint, listOf(olmSession))
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun DeviceCryptoSession.encrypt(messageJson: JsonString): EncryptionResult {
        interactWithOlm()
        val olmSession = this.olmSession as List<OlmSession>

        logger.crypto("encrypting with session(s) ${olmSession.size}")

        val (result, session) = olmSession.firstNotNullOf {
            kotlin.runCatching {
                it.encryptMessage(jsonCanonicalizer.canonicalize(messageJson)) to it
            }.getOrNull()
        }

        logger.crypto("encrypt flow identity: ${this.identity}")
        olmStore.persistSession(this.identity, SessionId(session.sessionIdentifier()), session)
        return EncryptionResult(
            cipherText = CipherText(result.mCipherText),
            type = result.mType,
        )
    }

    override suspend fun RoomCryptoSession.encrypt(roomId: RoomId, messageJson: JsonString): CipherText {
        interactWithOlm()
        val messagePayloadString = jsonCanonicalizer.canonicalize(messageJson)
        val outBound = this.outBound as OlmOutboundGroupSession
        val encryptedMessage = CipherText(outBound.encryptMessage(messagePayloadString))
        singletonFlows.update(
            "room-${roomId.value}",
            this.copy(outBound = outBound, messageIndex = outBound.messageIndex())
        )

        olmStore.persistOutbound(roomId, this.creationTimestampUtc, outBound)
        return encryptedMessage
    }

    private fun String.toSignedJson(olmAccount: OlmAccount): SignedJson {
        val json = JsonString(Json.encodeToString(mapOf("key" to this)))
        return SignedJson(olmAccount.signMessage(jsonCanonicalizer.canonicalize(json)))
    }

    override suspend fun decryptOlm(olmAccount: AccountCryptoSession, senderKey: Curve25519, type: Int, body: CipherText): DecryptionResult {
        interactWithOlm()
        val olmMessage = OlmMessage().apply {
            this.mType = type.toLong()
            this.mCipherText = body.value
        }

        val readSession = olmStore.readSessions(listOf(senderKey)).let {
            if (it == null) {
                logger.crypto("no olm session found for $senderKey, creating a new one")
                listOf(senderKey to OlmSession())
            } else {
                logger.crypto("found olm session(s) ${it.size}")
                it.forEach {
                    logger.crypto("${it.first} ${it.second.sessionIdentifier()}")
                }
                it
            }
        }
        val errors = mutableListOf<Throwable>()

        return readSession.firstNotNullOfOrNull { (_, session) ->
            kotlin.runCatching {
                when (type) {
                    OlmMessage.MESSAGE_TYPE_PRE_KEY -> {
                        if (session.matchesInboundSession(body.value)) {
                            logger.matrixLog(CRYPTO, "matched inbound session, attempting decrypt")
                            session.decryptMessage(olmMessage)?.let { JsonString(it) }
                        } else {
                            logger.matrixLog(CRYPTO, "prekey has no inbound session, doing alternative flow")
                            val account = olmAccount.olmAccount as OlmAccount

                            val session = OlmSession()
                            session.initInboundSessionFrom(account, senderKey.value, body.value)
                            account.removeOneTimeKeys(session)
                            olmAccount.updateAccountInstance(account)
                            session.decryptMessage(olmMessage)?.let { JsonString(it) }?.also {
                                logger.crypto("alt flow identity: $senderKey : ${session.sessionIdentifier()}")
                                olmStore.persistSession(senderKey, SessionId(session.sessionIdentifier()), session)
                            }.also {
                                session.releaseSession()
                            }
                        }
                    }
                    OlmMessage.MESSAGE_TYPE_MESSAGE -> {
                        logger.crypto("decrypting olm message type")
                        session.decryptMessage(olmMessage)?.let { JsonString(it) }
                    }
                    else -> throw IllegalArgumentException("Unknown message type: $type")
                }
            }.onFailure {
                errors.add(it)
                logger.crypto("error code: ${(it as? OlmException)?.exceptionCode}")
                errorTracker.track(it, "failed to decrypt olm")
            }.getOrNull()?.let { DecryptionResult.Success(it, isVerified = false) }
        }.ifNull {
            logger.matrixLog(CRYPTO, "failed to decrypt olm session")
            DecryptionResult.Failed(errors.joinToString { it.message ?: "N/A" })
        }.also {
            readSession.forEach { it.second.releaseSession() }
        }
    }

    private suspend fun AccountCryptoSession.updateAccountInstance(olmAccount: OlmAccount) {
        singletonFlows.update("account-crypto", this.copy(olmAccount = olmAccount))
        olmStore.persist(olmAccount)
    }

    override suspend fun decryptMegOlm(sessionId: SessionId, cipherText: CipherText): DecryptionResult {
        interactWithOlm()
        return when (val megolmSession = olmStore.readInbound(sessionId)) {
            null -> DecryptionResult.Failed("no megolm session found for id: $sessionId")
            else -> {
                runCatching {
                    JsonString(megolmSession.decryptMessage(cipherText.value).mDecryptedMessage).also {
                        olmStore.persist(sessionId, megolmSession)
                    }
                }.fold(
                    onSuccess = { DecryptionResult.Success(it, isVerified = false) },
                    onFailure = {
                        errorTracker.track(it)
                        DecryptionResult.Failed(it.message ?: "Unknown")
                    }
                ).also {
                    megolmSession.releaseSession()
                }
            }
        }
    }

    override suspend fun verifyExternalUser(keys: Ed25519?, recipeientKeys: Ed25519?): Boolean {
        return false
    }

    private suspend fun interactWithOlm() = singletonFlows.get<Unit>(INIT_OLM).first()

    override suspend fun olmSessions(devices: List<DeviceKeys>, onMissing: suspend (List<DeviceKeys>) -> List<DeviceCryptoSession>): List<DeviceCryptoSession> {
        interactWithOlm()

        val inputByIdentity = devices.groupBy { it.keys().first }
        val inputByKeys = devices.associateBy { it.keys() }

        val inputs = inputByKeys.map { (keys, deviceKeys) ->
            val (identity, fingerprint) = keys
            Olm.OlmSessionInput(oneTimeKey = "ignored", identity = identity, deviceKeys.deviceId, deviceKeys.userId, fingerprint)
        }

        val requestedIdentities = inputs.map { it.identity }
        val foundSessions = olmStore.readSessions(requestedIdentities) ?: emptyList()
        val foundSessionsByIdentity = foundSessions.groupBy { it.first }

        val foundSessionIdentities = foundSessions.map { it.first }
        val missingIdentities = requestedIdentities - foundSessionIdentities.toSet()

        val newOlmSessions = if (missingIdentities.isNotEmpty()) {
            onMissing(missingIdentities.map { inputByIdentity[it]!! }.flatten())
        } else emptyList()

        return (inputs.filterNot { missingIdentities.contains(it.identity) }.map {
            val olmSession = foundSessionsByIdentity[it.identity]!!.map { it.second }

            logger.crypto("found ${olmSession.size} olm session(s) for ${it.identity}")
            olmSession.forEach {
                logger.crypto(it.sessionIdentifier())
            }

            DeviceCryptoSession(
                deviceId = it.deviceId,
                userId = it.userId,
                identity = it.identity,
                fingerprint = it.fingerprint,
                olmSession = olmSession
            )
        }) + newOlmSessions
    }

    override suspend fun sasSession(deviceCredentials: DeviceCredentials): SasSession {
        val account = ensureAccountCrypto(deviceCredentials, onCreate = {})
        return DefaultSasSession(account.fingerprint)
    }
}


private fun DeviceKeys.keys(): Pair<Curve25519, Ed25519> {
    val identity = Curve25519(this.keys.filter { it.key.startsWith("curve25519:") }.values.first())
    val fingerprint = Ed25519(this.keys.filter { it.key.startsWith("ed25519:") }.values.first())
    return identity to fingerprint
}
