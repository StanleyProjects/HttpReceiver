package sp.kx.http

import java.util.UUID
import javax.crypto.SecretKey

internal fun mockTLSTransmitter(
    secretKey: SecretKey = MockSecretKey(),
    id: UUID = mockUUID(),
    body: ByteArray = mockByteArray(1),
): TLSTransmitter {
    return TLSTransmitter(
        secretKey = secretKey,
        id = id,
        body = body,
    )
}
