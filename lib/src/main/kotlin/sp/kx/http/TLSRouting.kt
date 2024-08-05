package sp.kx.http

import java.security.KeyPair
import java.util.UUID
import kotlin.time.Duration

abstract class TLSRouting(
    private val env: TLSEnvironment,
) : HttpRouting {
    abstract val keyPair: KeyPair
    abstract var requested: Map<UUID, Duration>

    private fun onReceiver(request: TLSReceiver) {
        val now = env.now()
        // todo now < time
        if (now - request.time > env.maxTime) error("Time error!")
        val requested = this.requested
        if (requested.containsKey(request.id)) {
            error("Request ID error!")
        } else if (requested.any { (_, it) -> now - it > env.maxTime }) {
            this.requested = requested.filterValues { now - it > env.maxTime}
        }
        this.requested += request.id to request.time
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
//            println("error: $error") // todo
            HttpResponse.InternalServerError(body = "todo".toByteArray())
        }
    }
}
