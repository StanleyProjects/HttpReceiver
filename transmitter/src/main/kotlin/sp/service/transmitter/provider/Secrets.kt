package sp.service.transmitter.provider

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey

internal interface Secrets {
    fun hash(bytes: ByteArray): ByteArray
    fun newSecretKey(): SecretKey
    fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray
    fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray
    fun sign(key: PrivateKey, encoded: ByteArray): ByteArray
    fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean
}
