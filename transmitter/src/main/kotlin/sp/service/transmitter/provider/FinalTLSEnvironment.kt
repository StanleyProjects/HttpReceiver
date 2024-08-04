package sp.service.transmitter.provider

import sp.kx.http.TLSEnvironment
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class FinalTLSEnvironment(
    private val secrets: Secrets,
    override val maxTime: Duration,
) : TLSEnvironment {
    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }

    override fun newUUID(): UUID {
        return UUID.randomUUID()
    }

    override fun newSecretKey(): SecretKey {
        return secrets.newSecretKey()
    }

    override fun toSecretKey(encoded: ByteArray): SecretKey {
        return secrets.toSecretKey(encoded)
    }

    override fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray {
        return secrets.encrypt(key, decrypted)
    }

    override fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray {
        return secrets.encrypt(key, decrypted)
    }

    override fun sign(key: PrivateKey, encoded: ByteArray): ByteArray {
        return secrets.sign(key, encoded)
    }

    override fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray {
        return secrets.decrypt(key, encrypted)
    }

    override fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        return secrets.decrypt(key, encrypted)
    }

    override fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean {
        return secrets.verify(key, encoded, signature)
    }
}
