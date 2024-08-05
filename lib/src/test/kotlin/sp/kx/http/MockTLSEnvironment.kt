package sp.kx.http

import java.security.PrivateKey
import java.security.PublicKey
import javax.crypto.SecretKey
import kotlin.time.Duration

internal class MockTLSEnvironment(
    override val maxTime: Duration = Duration.ZERO,
) : TLSEnvironment {
    override fun now(): Duration {
        TODO("now")
    }

    override fun newSecretKey(): SecretKey {
        TODO("newSecretKey")
    }

    override fun toSecretKey(encoded: ByteArray): SecretKey {
        TODO("toSecretKey")
    }

    override fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray {
        TODO("encrypt")
    }

    override fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray {
        TODO("encrypt")
    }

    override fun sign(key: PrivateKey, encoded: ByteArray): ByteArray {
        TODO("sign")
    }

    override fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray {
        TODO("decrypt")
    }

    override fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        TODO("decrypt")
    }

    override fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean {
        TODO("verify")
    }
}
