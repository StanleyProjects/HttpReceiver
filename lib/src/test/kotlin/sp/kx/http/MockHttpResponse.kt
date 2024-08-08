package sp.kx.http

internal fun mockHttpResponse(
    version: String = "",
    code: Int = -1,
    message: String = "",
    headers: Map<String, String> = emptyMap(),
    body: ByteArray? = null,
): HttpResponse {
    return HttpResponse(
        version = version,
        code = code,
        message = message,
        headers = headers,
        body = body,
    )
}
