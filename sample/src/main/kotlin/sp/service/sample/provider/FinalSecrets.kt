package sp.service.sample.provider

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

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

    override fun newSecretKey(): SecretKey {
        val generator = KeyGenerator.getInstance("AES")
        return generator.generateKey()
    }

    override fun encrypt(publicKey: PublicKey, decrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(decrypted)
    }

    override fun encrypt(secretKey: SecretKey, decrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(decrypted)
    }

    override fun decrypt(secretKey: SecretKey, encrypted: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(encrypted)
    }
}
