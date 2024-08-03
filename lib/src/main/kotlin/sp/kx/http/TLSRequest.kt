package sp.kx.http

import java.util.UUID
import javax.crypto.SecretKey

internal class TLSRequest(
    val secretKey: SecretKey,
    val id: UUID,
    val encoded: ByteArray,
) {
    override fun toString(): String {
        return "{" +
            "secretKey.size: ${secretKey.encoded.size}, " +
            "id: $id, " +
            "encoded.size: ${encoded.size}, " +
            "}"
    }
}
