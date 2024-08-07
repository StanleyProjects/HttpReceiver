package sp.kx.http

import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration

interface TLSEnvironment {
    fun now(): Duration
    val timeMax: Duration
    fun newUUID(): UUID
    fun newSecretKey(): SecretKey
    fun toSecretKey(encoded: ByteArray): SecretKey
    fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray
    fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray
    fun sign(key: PrivateKey, encoded: ByteArray): ByteArray
    fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray
    fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean

    companion object {
        fun getMethodCode(method: String): Byte {
            return when (method) {
                "POST" -> 1
                else -> error("Method \"${method}\" is not supported!")
            }
        }
    }
}
