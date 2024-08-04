package sp.service.sample.provider

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

internal interface Secrets {
    fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray
    fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray
    fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean
    fun sign(key: PrivateKey, encoded: ByteArray): ByteArray
    fun hash(bytes: ByteArray): ByteArray
}
