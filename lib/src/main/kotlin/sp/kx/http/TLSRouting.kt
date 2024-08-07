package sp.kx.http

import java.security.KeyPair
import java.util.UUID
import kotlin.time.Duration

abstract class TLSRouting(
    private val env: TLSEnvironment,
) : HttpRouting {
    protected abstract val keyPair: KeyPair
    protected abstract var requested: Map<UUID, Duration>

    internal fun onReceiver(receiver: TLSReceiver) {
        val timeNow = env.now()
        if (timeNow < receiver.time) error("Time error!")
        if (timeNow - receiver.time > env.timeMax) error("Time is up!")
        val requested = this.requested
        if (requested.containsKey(receiver.id)) {
            error("Request ID error!")
        } else if (requested.any { (_, it) -> timeNow - it > env.timeMax }) {
            this.requested = requested.filterValues { timeNow - it < env.timeMax }
        }
        this.requested += receiver.id to receiver.time
    }

    protected fun <REQ : Any, RES : Any> map(
        request: HttpRequest,
        decode: (ByteArray) -> REQ,
        transform: (REQ) -> RES,
        encode: (RES) -> ByteArray,
    ): HttpResponse {
        return runCatching {
            val body = request.body ?: error("No body!")
            val methodCode = TLSEnvironment.getMethodCode(method = request.method)
            val encodedQuery = request.query.toByteArray()
            val tlsReceiver = TLSReceiver.build(
                env = env,
                keyPair = keyPair,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                body = body,
            )
            onReceiver(tlsReceiver)
            val decoded = decode(tlsReceiver.encoded)
//            println("decoded: $decoded") // todo
            TLSReceiver.toResponseBody(
                env = env,
                secretKey = tlsReceiver.secretKey,
                privateKey = keyPair.private,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                requestID = tlsReceiver.id,
                encoded = encode(transform(decoded)),
            )
        }.map { body ->
            HttpResponse.OK(body = body)
        }.getOrElse { error ->
            error("error: $error") // todo
            HttpResponse.InternalServerError(body = "todo".toByteArray())
        }
    }
}
