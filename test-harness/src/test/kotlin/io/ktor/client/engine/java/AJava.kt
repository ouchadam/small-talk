package io.ktor.client.engine.java

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.util.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Work around to disable SSL for local syanpse testing
 * Ktor loads http engines by name, hence the JavaHttpEngineContainer name being 1
 */
@OptIn(InternalAPI::class)
object TestJava : HttpClientEngineFactory<JavaHttpConfig> {
    init {
        System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", "true")
    }

    override fun create(block: JavaHttpConfig.() -> Unit): HttpClientEngine {
        val config = JavaHttpConfig().apply(block).also {
            it.config {
                val apply = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf(TrustAllX509TrustManager()), SecureRandom())
                }
                sslContext(apply)
            }
        }
        return JavaHttpEngine(config)
    }
}

@Suppress("KDocMissingDocumentation")
class JavaHttpEngineContainer : HttpClientEngineContainer {
    override val factory: HttpClientEngineFactory<*> = TestJava

    override fun toString(): String = "1"
}

private class TrustAllX509TrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate?>? = null
    override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
    override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
}