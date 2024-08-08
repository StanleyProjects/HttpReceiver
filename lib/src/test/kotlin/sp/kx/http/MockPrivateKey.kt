package sp.kx.http

import java.security.PrivateKey

internal class MockPrivateKey(
    private val encoded: ByteArray = ByteArray(0),
) : PrivateKey {
    override fun getAlgorithm(): String {
        TODO("MockPrivateKey;getAlgorithm")
    }

    override fun getFormat(): String {
        TODO("MockPrivateKey;getFormat")
    }

    override fun getEncoded(): ByteArray {
        return encoded
    }
}
