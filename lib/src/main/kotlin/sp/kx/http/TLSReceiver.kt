package sp.kx.http

import sp.kx.bytes.toHEX
import java.util.UUID
import javax.crypto.SecretKey

internal class TLSReceiver(
    val secretKey: SecretKey,
    val id: UUID,
    val encoded: ByteArray,
) {
    override fun toString(): String {
        return "{" +
                "secretKey: ${secretKey.encoded.toHEX()}, " +
                "id: $id, " +
                "encoded: ${encoded.toHEX()}, " +
                "}"
    }
}
