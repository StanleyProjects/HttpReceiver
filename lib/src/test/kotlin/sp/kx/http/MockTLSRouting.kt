package sp.kx.http

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration

internal class MockTLSRouting(
    override var requested: Map<UUID, Duration> = emptyMap(),
    private val keyPairProvider: MockProvider<KeyPair> = MockProvider { TODO("MockTLSRouting:keyPairProvider") },
) : TLSRouting() {
    override fun getKeyPair(): KeyPair {
        return keyPairProvider.provide()
    }

    override fun toSecretKey(encoded: ByteArray): SecretKey {
        TODO("toSecretKey")
    }

    override fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray {
        TODO("decrypt")
    }

    override fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        TODO("decrypt")
    }

    override fun encrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        TODO("encrypt")
    }

    override fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean {
        TODO("verify")
    }

    override fun sign(key: PrivateKey, encoded: ByteArray): ByteArray {
        TODO("sign")
    }

    override fun getMaxTime(): Duration {
        TODO("getMaxTime")
    }

    override fun now(): Duration {
        TODO("now")
    }

    override fun route(request: HttpRequest): HttpResponse {
        TODO("route")
    }
}
