package sp.service.sample.provider

import java.security.PublicKey
import javax.crypto.SecretKey

internal interface Secrets {
    fun toPublicKey(encoded: ByteArray): PublicKey
    fun hash(bytes: ByteArray): String
    fun newSecretKey(): SecretKey
    fun encrypt(publicKey: PublicKey, decrypted: ByteArray): ByteArray
    fun encrypt(secretKey: SecretKey, decrypted: ByteArray): ByteArray
    fun decrypt(secretKey: SecretKey, encrypted: ByteArray): ByteArray
}
