package sp.service.sample.provider

import sp.kx.http.TLSEnvironment
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class FinalTLSEnvironment(override val maxTime: Duration) : TLSEnvironment {
    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun newUUID(): UUID {
        return UUID.randomUUID()
    }

    override fun newSecretKey(): SecretKey {
        return KeyGenerator.getInstance("AES").generateKey()
    }

    override fun toSecretKey(encoded: ByteArray): SecretKey {
        return SecretKeySpec(encoded, "AES")
    }

    override fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(decrypted)
    }

    override fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(decrypted)
    }

    override fun sign(key: PrivateKey, encoded: ByteArray): ByteArray {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(key)
        sig.update(encoded)
        return sig.sign()
    }

    override fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(encrypted)
    }

    override fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(encrypted)
    }

    override fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(key)
        sig.update(encoded)
        return sig.verify(signature)
    }
}
