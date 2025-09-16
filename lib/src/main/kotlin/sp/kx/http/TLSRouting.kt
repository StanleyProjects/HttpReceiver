package sp.kx.http

import sp.kx.tlsmessages.TLSReceiver

abstract class TLSRouting(
    private val receiver: TLSReceiver,
) : HttpRouting {
    protected fun map(
        request: HttpRequest,
        transform: (ByteArray) -> HttpResponse,
    ): HttpResponse {
        return try {
            val req = receiver.fromRequest(
                method = request.method,
                query = request.query,
                bytes = request.body ?: error("No body!"),
            )
            val decoded = transform(req.body)
            HttpResponse(
                version = "1.1",
                code = decoded.code,
                message = decoded.message,
                headers = decoded.headers,
                body = receiver.toResponseBody(
                    code = decoded.code,
                    message = decoded.message,
                    body = decoded.body,
                    issuer = req.issuer,
                ),
            )
        } catch (error: Throwable) {
            recover(error = error)
        }
    }

    protected open fun recover(error: Throwable): HttpResponse {
        val text = "The reasons for the error are hidden for security reasons."
        return HttpResponse.InternalServerError(body = text.toByteArray())
    }
}
