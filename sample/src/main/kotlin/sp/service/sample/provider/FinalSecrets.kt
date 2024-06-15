package sp.service.sample.provider

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

internal class FinalSecrets : Secrets {
    override fun toPublicKey(encoded: ByteArray): PublicKey {
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(encoded)
        return keyFactory.generatePublic(keySpec)
    }

    override fun hash(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA256")
        return md.digest(bytes).joinToString(separator = "") { String.format("%02x", it.toInt() and 0xff) }
    }
}
