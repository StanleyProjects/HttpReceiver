package sp.service.sample.provider

import java.security.PublicKey

internal interface Secrets {
    fun toPublicKey(encoded: ByteArray): PublicKey
    fun hash(bytes: ByteArray): String
}
