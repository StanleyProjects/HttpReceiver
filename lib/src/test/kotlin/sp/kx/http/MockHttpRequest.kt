package sp.kx.http

internal fun mockHttpRequest(
    version: String = "",
    method: String = "",
    query: String = "",
    headers: Map<String, String> = emptyMap(),
    body: ByteArray? = null,
): HttpRequest {
    return HttpRequest(
        version = version,
        method = method,
        query = query,
        headers = headers,
        body = body,
    )
}
