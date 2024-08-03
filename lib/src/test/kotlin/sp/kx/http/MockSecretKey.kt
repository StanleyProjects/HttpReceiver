package sp.kx.http

import javax.crypto.SecretKey

internal class MockSecretKey(
    private val encoded: ByteArray = ByteArray(0),
) : SecretKey {
    override fun getAlgorithm(): String {
        TODO("MockSecretKey:getAlgorithm")
    }

    override fun getFormat(): String {
        TODO("MockSecretKey:getFormat")
    }

    override fun getEncoded(): ByteArray {
        return encoded
    }
}
