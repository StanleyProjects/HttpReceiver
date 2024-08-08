package sp.kx.http

import java.util.UUID
import kotlin.time.Duration

abstract class TLSRouting(
    private val env: TLSEnvironment,
) : HttpRouting {
    protected abstract var requested: Map<UUID, Duration>

    internal fun onReceiver(receiver: TLSReceiver) {
        val timeNow = env.now()
//        if (timeNow < receiver.time) error("Time error!") // todo IEEE 1588 Precision Time Protocol
        if (timeNow - receiver.time > env.timeMax) error("Time is up!")
        val requested = this.requested
        if (requested.containsKey(receiver.id)) {
            error("Request ID error!")
        } else if (requested.any { (_, it) -> timeNow - it > env.timeMax }) {
            this.requested = requested.filterValues { timeNow - it < env.timeMax }
        }
        this.requested += receiver.id to receiver.time
    }

    protected fun map(
        request: HttpRequest,
        transform: (ByteArray) -> TLSResponse,
    ): HttpResponse {
        return runCatching {
            val body = request.body ?: error("No body!")
            val methodCode = TLSEnvironment.getMethodCode(method = request.method)
            val encodedQuery = request.query.toByteArray()
            val tlsReceiver = TLSReceiver.build(
                env = env,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                body = body,
            )
            onReceiver(tlsReceiver)
            val tlsResponse = transform(tlsReceiver.encoded)
            TLSReceiver.toHttpResponse(
                env = env,
                secretKey = tlsReceiver.secretKey,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                requestID = tlsReceiver.id,
                tlsResponse = tlsResponse,
            )
        }.getOrElse { error ->
//            error("error: $error") // todo
            HttpResponse.InternalServerError(body = "todo".toByteArray())
        }
    }
}
