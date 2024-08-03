package sp.kx.http

import java.util.UUID
import javax.crypto.SecretKey

internal fun mockTLSRequest(
    secretKey: SecretKey = MockSecretKey(),
    id: UUID = mockUUID(),
    encoded: ByteArray = ByteArray(0),
): TLSRequest {
    return TLSRequest(
        secretKey = secretKey,
        id = id,
        encoded = encoded,
    )
}
