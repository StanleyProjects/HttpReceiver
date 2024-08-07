package sp.kx.http

import java.security.KeyPair
import java.util.UUID
import kotlin.time.Duration

internal class MockTLSRouting(
    override val keyPair: KeyPair = mockKeyPair(),
    override var requested: Map<UUID, Duration> = emptyMap(),
    private val mapping: Map<String, Map<String, (HttpRequest) -> HttpResponse>> = emptyMap(),
    env: TLSEnvironment = MockTLSEnvironment(),
) : TLSRouting(env = env) {
    override fun route(request: HttpRequest): HttpResponse {
        val routes = mapping[request.query] ?: TODO("MockTLSRouting:route(${request.query})")
        val route = routes[request.method] ?: TODO("MockTLSRouting:route(${request.method}/${request.query})")
        return route(request)
    }

    fun requested(): Map<UUID, Duration> {
        return requested
    }

    fun <REQ : Any, RES : Any> test(
        request: HttpRequest = mockHttpRequest(),
        decode: (ByteArray) -> REQ = {TODO("MockTLSRouting:test:decode")},
        transform: (REQ) -> RES = {TODO("MockTLSRouting:test:transform")},
        encode: (RES) -> ByteArray = {TODO("MockTLSRouting:test:encode")},
    ): HttpResponse {
        return map(
            request = request,
            decode = decode,
            transform = transform,
            encode = encode,
        )
    }
}
