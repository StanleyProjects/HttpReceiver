package sp.kx.http

import java.util.UUID
import kotlin.time.Duration

internal class MockTLSRouting(
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

    fun test(
        request: HttpRequest = mockHttpRequest(),
        transform: (ByteArray) -> TLSResponse = { TODO("MockTLSRouting:test:transform") },
    ): HttpResponse {
        return map(
            request = request,
            transform = transform,
        )
    }
}
