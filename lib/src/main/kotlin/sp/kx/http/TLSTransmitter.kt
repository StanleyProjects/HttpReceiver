package sp.kx.http

import sp.kx.bytes.toHEX
import java.util.UUID
import javax.crypto.SecretKey

class TLSTransmitter(
    val secretKey: SecretKey,
    val id: UUID,
    val body: ByteArray,
) {
    override fun toString(): String {
        return "{" +
            "secretKey: ${secretKey.encoded.toHEX()}, " +
            "id: $id, " +
            "body: ${body.toHEX()}, " +
            "}"
    }
}
