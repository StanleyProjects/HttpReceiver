package sp.kx.http

import java.util.UUID
import javax.crypto.SecretKey

internal class TLSRequest<T : Any>(
    val secretKey: SecretKey,
    val methodCode: Byte,
    val encodedQuery: ByteArray,
    val id: UUID,
    val decoded: T,
)
