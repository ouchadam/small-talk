package app.dapk.st.matrix.crypto.internal

import app.dapk.st.core.Base64
import app.dapk.st.core.CoroutineDispatchers
import app.dapk.st.matrix.common.AlgorithmName
import app.dapk.st.matrix.common.RoomId
import app.dapk.st.matrix.common.SessionId
import app.dapk.st.matrix.common.SharedRoomKey
import app.dapk.st.matrix.crypto.ImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

private const val HEADER_LINE = "-----BEGIN MEGOLM SESSION DATA-----"
private const val TRAILER_LINE = "-----END MEGOLM SESSION DATA-----"
private val importJson = Json { ignoreUnknownKeys = true }

class RoomKeyImporter(
    private val base64: Base64,
    private val dispatchers: CoroutineDispatchers,
) {

    suspend fun InputStream.importRoomKeys(password: String, onChunk: suspend (List<SharedRoomKey>) -> Unit): Flow<ImportResult> {
        return flow {
            runCatching { this@importRoomKeys.import(password, onChunk, this) }
                .onFailure {
                    when (it) {
                        is ImportException -> emit(ImportResult.Error(it.type))
                        else -> emit(ImportResult.Error(ImportResult.Error.Type.Unknown(it)))
                    }
                }
        }.flowOn(dispatchers.io)
    }

    private suspend fun InputStream.import(password: String, onChunk: suspend (List<SharedRoomKey>) -> Unit, collector: FlowCollector<ImportResult>) {
        var importedKeysCount = 0L
        val roomIds = mutableSetOf<RoomId>()

        this.bufferedReader().use {
            with(JsonAccumulator()) {
                it.useLines { sequence ->
                    sequence
                        .filterNot { it == HEADER_LINE || it == TRAILER_LINE || it.isEmpty() }
                        .chunked(5)
                        .decrypt(password)
                        .accumulateJson()
                        .map { decoded ->
                            roomIds.add(decoded.roomId)
                            SharedRoomKey(
                                decoded.algorithmName,
                                decoded.roomId,
                                decoded.sessionId,
                                decoded.sessionKey,
                                isExported = true,
                            )
                        }
                        .chunked(500)
                        .forEach {
                            onChunk(it)
                            importedKeysCount += it.size
                            collector.emit(ImportResult.Update(importedKeysCount))
                        }
                }
            }
            when {
                roomIds.isEmpty() -> collector.emit(ImportResult.Error(ImportResult.Error.Type.NoKeysFound))
                else -> collector.emit(ImportResult.Success(roomIds, importedKeysCount))
            }
        }
    }

    private fun Sequence<List<String>>.decrypt(password: String): Sequence<String> {
        val decryptCipher = Cipher.getInstance("AES/CTR/NoPadding")
        return this.withIndex().map { (index, it) ->
            val line = it.joinToString(separator = "").replace("\n", "")
            val toByteArray = base64.decode(line)
            if (index == 0) {
                decryptCipher.initialize(toByteArray, password)
                toByteArray
                    .copyOfRange(37, toByteArray.size)
                    .decrypt(decryptCipher)
                    .also {
                        if (!it.startsWith("[{")) {
                            throw ImportException(ImportResult.Error.Type.UnexpectedDecryptionOutput)
                        }
                    }
            } else {
                toByteArray.decrypt(decryptCipher)
            }
        }
    }

    private fun Cipher.initialize(payload: ByteArray, passphrase: String) {
        val salt = payload.copyOfRange(1, 1 + 16)
        val iv = payload.copyOfRange(17, 17 + 16)
        val iterations = (payload[33].toUnsignedInt() shl 24) or
                (payload[34].toUnsignedInt() shl 16) or
                (payload[35].toUnsignedInt() shl 8) or
                payload[36].toUnsignedInt()
        val deriveKey = deriveKeys(salt, iterations, passphrase)
        val secretKeySpec = SecretKeySpec(deriveKey.getAesKey(), "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        this.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
    }

    private fun ByteArray.decrypt(cipher: Cipher): String {
        return cipher.update(this).toString(Charset.defaultCharset())
    }

    private fun ByteArray.getAesKey() = this.copyOfRange(0, 32)

    private fun deriveKeys(salt: ByteArray, iterations: Int, password: String): ByteArray {
        val prf = Mac.getInstance("HmacSHA512")
        prf.init(SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA512"))

        // 512 bits key length
        val key = ByteArray(64)
        val uc = ByteArray(64)

        // U1 = PRF(Password, Salt || INT_32_BE(i))
        prf.update(salt)
        val int32BE = ByteArray(4) { 0.toByte() }
        int32BE[3] = 1.toByte()
        prf.update(int32BE)
        prf.doFinal(uc, 0)

        // copy to the key
        System.arraycopy(uc, 0, key, 0, uc.size)

        for (index in 2..iterations) {
            // Uc = PRF(Password, Uc-1)
            prf.update(uc)
            prf.doFinal(uc, 0)

            // F(Password, Salt, c, i) = U1 ^ U2 ^ ... ^ Uc
            for (byteIndex in uc.indices) {
                key[byteIndex] = key[byteIndex] xor uc[byteIndex]
            }
        }
        return key
    }
}

private fun Byte.toUnsignedInt() = toInt() and 0xff

@Serializable
private data class ElementMegolmExportObject(
    @SerialName("room_id") val roomId: RoomId,
    @SerialName("session_key") val sessionKey: String,
    @SerialName("session_id") val sessionId: SessionId,
    @SerialName("algorithm") val algorithmName: AlgorithmName,
)

private class ImportException(val type: ImportResult.Error.Type) : Throwable()

private class JsonAccumulator {

    private var jsonSegment = ""

    fun <T> Sequence<T>.accumulateJson() = this.mapNotNull {
        val withLatest = jsonSegment + it
        try {
            when (val objectRange = withLatest.findClosingIndex()) {
                null -> {
                    jsonSegment = withLatest
                    null
                }
                else -> {
                    val string = withLatest.substring(objectRange)
                    importJson.decodeFromString(ElementMegolmExportObject.serializer(), string).also {
                        jsonSegment = withLatest.replace(string, "").removePrefix(",")
                    }
                }
            }
        } catch (error: Throwable) {
            jsonSegment = withLatest
            null
        }
    }

    private fun String.findClosingIndex(): IntRange? {
        var opens = 0
        var openIndex = -1
        this.forEachIndexed { index, c ->
            when {
                c == '{' -> {
                    if (opens == 0) {
                        openIndex = index
                    }
                    opens++
                }
                c == '}' -> {
                    opens--
                    if (opens == 0) {
                        return IntRange(openIndex, index)
                    }
                }
            }
        }
        return null
    }
}