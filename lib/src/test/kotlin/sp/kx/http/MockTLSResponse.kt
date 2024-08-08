package sp.kx.http

internal fun mockTLSResponse(
    code: Int = 42,
    message: String = "foobarbaz",
    encoded: ByteArray? = null,
): TLSResponse {
    return TLSResponse(
        code = code,
        message = message,
        encoded = encoded,
    )
}
