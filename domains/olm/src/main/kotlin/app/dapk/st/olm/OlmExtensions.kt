package app.dapk.st.olm

import app.dapk.st.matrix.common.Curve25519
import app.dapk.st.matrix.common.Ed25519
import org.matrix.olm.OlmAccount

fun OlmAccount.readIdentityKeys(): Pair<Ed25519, Curve25519> {
    val identityKeys = this.identityKeys()
    return Ed25519(identityKeys["ed25519"]!!) to Curve25519(identityKeys["curve25519"]!!)
}

fun OlmAccount.oneTimeCurveKeys(): List<Pair<String, Curve25519>> {
    return this.oneTimeKeys()["curve25519"]?.map { it.key to Curve25519(it.value) } ?: emptyList()
}