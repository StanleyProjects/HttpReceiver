package sp.kx.http

import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

internal fun mockKeyPair(
    publicKey: PublicKey = MockPublicKey(),
    privateKey: PrivateKey = MockPrivateKey(),
): KeyPair {
    return KeyPair(publicKey, privateKey)
}
