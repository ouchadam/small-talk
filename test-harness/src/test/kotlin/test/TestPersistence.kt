package test

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.*

class TestPersistence(
    prefix: String,
) {

    private val dir = File("build/st-test-persistence/${prefix}")

    init {
        dir.mkdirs()
    }

    suspend fun <T> put(fileName: String, serializer: KSerializer<T>, value: T) {
        writeFile(fileName, Json.encodeToString(serializer, value))
    }

    suspend fun <T> readOrPut(fileName: String, serializer: KSerializer<T>, storeAction: suspend () -> T): T {
        val file = File(dir, fileName)

        return if (file.exists()) {
            Json.decodeFromString(serializer, file.readText()).also {
                println("restored $it from $fileName")
            }
        } else {
            storeAction().also { result ->
                writeFile(fileName, Json.encodeToString(serializer, result))
            }
        }
    }

    suspend fun <T : Serializable> read(fileName: String): T? {
        val file = File(dir, fileName)

        return if (file.exists()) {
            val text = file.readBytes()
            val decoded = Base64.getDecoder().decode(text)
            ObjectInputStream(ByteArrayInputStream(decoded)).use {
                it.readObject() as T
            }.also {
                println("restored $it from $fileName")
            }
        } else {
            null
        }
    }

    suspend fun readJson(fileName: String): String? {
        val file = File(dir, fileName)

        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    private fun writeFile(name: String, jsonContent: String) {
        ensureFile(name).writeText(jsonContent)
    }

    private fun ensureFile(fileName: String) = File(dir, fileName).also {
        it.parentFile?.mkdirs()
    }
}