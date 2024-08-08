package sp.kx.http

import sp.kx.bytes.toHEX
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration

internal class MockTLSEnvironment(
    override val timeMax: Duration = Duration.ZERO,
    override val keyPair: KeyPair = KeyPair(MockPublicKey(mockByteArray(2)), MockPrivateKey(mockByteArray(4))),
    private val timeProvider: MockProvider<Duration> = MockProvider { Duration.ZERO },
    private val newUUIDProvider: MockProvider<UUID> = MockProvider { mockUUID() },
    private val newSecretKeyProvider: MockProvider<SecretKey> = MockProvider { MockSecretKey() },
    private val items: List<Triple<ByteArray, ByteArray, ByteArray>> = emptyList(),
    private val keys: List<Pair<ByteArray, SecretKey>> = emptyList(),
    private val signs: List<Pair<KeyPair, Pair<ByteArray, ByteArray>>> = emptyList(),
) : TLSEnvironment {
    override fun now(): Duration {
        return timeProvider.provide()
    }

    override fun newUUID(): UUID {
        return newUUIDProvider.provide()
    }

    override fun newSecretKey(): SecretKey {
        return newSecretKeyProvider.provide()
    }

    override fun toSecretKey(encoded: ByteArray): SecretKey {
        for ((e, key) in keys) {
            if (e.contentEquals(encoded)) {
                return key
            }
        }
        TODO("MockTLSEnvironment:toSecretKey(encoded: ${encoded.toHEX()})")
    }

    override fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray {
        for ((ek, d, e) in items) {
            if (d.contentEquals(decrypted) && ek.contentEquals(key.encoded)) {
                return e
            }
        }
        TODO("MockTLSEnvironment:(async)encrypt(key: ${key.encoded.toHEX()}, decrypted: ${decrypted.toHEX()})")
    }

    override fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray {
        for ((ek, d, e) in items) {
            if (d.contentEquals(decrypted) && ek.contentEquals(key.encoded)) {
                return e
            }
        }
        TODO("MockTLSEnvironment:encrypt(key(${key.encoded.size}): ${key.encoded.toHEX()}, decrypted(${decrypted.size}): ${decrypted.toHEX()})\n${items.map { (k, d, e) -> "\n\tk(${k.size}): ${k.toHEX()}\n\td(${d.size}): ${d.toHEX()}\n\te(${e.size}): ${e.toHEX()}" }}")
    }

    override fun sign(key: PrivateKey, encoded: ByteArray): ByteArray {
        for ((keyPair, it) in signs) {
            val (e, s) = it
            if (e.contentEquals(encoded) && keyPair.private.encoded.contentEquals(key.encoded)) {
                return s
            }
        }
        TODO("MockTLSEnvironment:sign(key: ${key.encoded.toHEX()}, encoded: ${encoded.toHEX()})\n${signs.map { (_, it) -> "e: ${it.first.toHEX()}, s: ${it.second.toHEX()}" }}")
    }

    override fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray {
        for ((ek, d, e) in items) {
            if (e.contentEquals(encrypted) && ek.contentEquals(key.encoded)) {
                return d
            }
        }
        TODO("MockTLSEnvironment:decrypt(key: ${key.encoded.toHEX()}, encrypted: ${encrypted.toHEX()})")
    }

    override fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        for ((ek, d, e) in items) {
            if (e.contentEquals(encrypted) && ek.contentEquals(key.encoded)) {
                return d
            }
        }
        TODO("MockTLSEnvironment:decrypt(key: ${key.encoded.toHEX()}, encrypted: ${encrypted.toHEX()})")
    }

    override fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean {
        for ((keyPair, it) in signs) {
            val (e, s) = it
            if (e.contentEquals(encoded) && keyPair.public.encoded.contentEquals(key.encoded)) {
                return s.contentEquals(signature)
            }
        }
        TODO("MockTLSEnvironment:verify(key(${key.encoded.size}): ${key.encoded.toHEX()}, encoded(${encoded.size}): ${encoded.toHEX()}, signature(${signature.size}): ${signature.toHEX()})\n${signs.map { (k, it) -> "\n\tk(${k.public.encoded.size}): ${k.public.encoded.toHEX()}\n\te(${it.first.size}): ${it.first.toHEX()}\n\ts(${it.second.size}): ${it.second.toHEX()}" }}")
    }
}
