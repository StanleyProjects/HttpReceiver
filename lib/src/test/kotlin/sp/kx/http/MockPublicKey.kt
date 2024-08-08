package sp.kx.http

import java.security.PublicKey

internal class MockPublicKey(
    private val encoded: ByteArray = ByteArray(0),
) : PublicKey {
    override fun getAlgorithm(): String {
        TODO("MockPublicKey:getAlgorithm")
    }

    override fun getFormat(): String {
        TODO("MockPublicKey:getFormat")
    }

    override fun getEncoded(): ByteArray {
        return encoded
    }
}
