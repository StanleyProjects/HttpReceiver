package sp.kx.http

import sp.kx.tlsmessages.TLSIssuer
import java.util.UUID
import kotlin.time.Duration
import sp.kx.tlsmessages.TLSReceiver
import sp.kx.tlsmessages.TLSRequest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

abstract class TLSRouting(
    private val receiver: TLSReceiver,
) : HttpRouting {
    protected abstract var requested: Map<UUID, Duration>

    private fun onReceiver(req: TLSRequest.Decoded) {
        val timeNow = System.currentTimeMillis().milliseconds // todo
//        if (timeNow < receiver.time) error("Time error!") // todo IEEE 1588 Precision Time Protocol
        val timeMax = 1.minutes // todo
        if (timeNow - req.time > timeMax) error("Time is up!")
        if (requested.containsKey(req.issuer.id)) error("Request ID error!")
        requested = requested.filterValues { timeNow - it < timeMax }
        requested += req.issuer.id to req.time
    }

    private fun TLSReceiver.encode(
        decoded: HttpResponse,
        issuer: TLSIssuer,
    ): HttpResponse {
        return HttpResponse(
            version = "1.1",
            code = decoded.code,
            message = decoded.message,
            headers = decoded.headers,
            body = toResponseBody(
                code = decoded.code,
                message = decoded.message,
                body = decoded.body,
                issuer = issuer,
            ),
        )
    }

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
            onReceiver(req)
            receiver.encode(
                decoded = transform(req.body),
                issuer = req.issuer,
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
