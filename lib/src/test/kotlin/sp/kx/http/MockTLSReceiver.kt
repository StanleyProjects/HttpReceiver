package sp.kx.http

import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration

internal fun mockTLSReceiver(
    secretKey: SecretKey = MockSecretKey(),
    id: UUID = mockUUID(),
    time: Duration = Duration.ZERO,
    encoded: ByteArray = ByteArray(0),
): TLSReceiver {
    return TLSReceiver(
        secretKey = secretKey,
        id = id,
        time = time,
        encoded = encoded,
    )
}
