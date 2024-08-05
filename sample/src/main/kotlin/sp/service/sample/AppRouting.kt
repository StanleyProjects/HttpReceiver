package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import sp.kx.bytes.readInt
import sp.kx.bytes.write
import sp.kx.http.HttpRequest
import sp.kx.http.HttpResponse
import sp.kx.http.TLSEnvironment
import sp.kx.http.TLSReceiver
import sp.kx.http.TLSRouting
import sp.service.sample.provider.Loggers
import java.security.KeyPair
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class AppRouting(
    loggers: Loggers,
    tlsEnv: TLSEnvironment,
    override val keyPair: KeyPair,
    override var requested: Map<UUID, Duration>,
    private val coroutineScope: CoroutineScope,
) : TLSRouting(tlsEnv) {
    sealed interface Event {
        data object Quit : Event
    }

    private val logger = loggers.create("[Routing]")
    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val mapping = mapOf(
        "/double" to mapOf(
            "POST" to ::onPostDouble,
        ),
        "/quit" to mapOf(
            "GET" to ::onGetQuit,
        ),
    )

    private fun onGetQuit(request: HttpRequest): HttpResponse {
        coroutineScope.launch {
            _events.emit(Event.Quit)
        }
        return HttpResponse.OK()
    }

    private fun onPostDouble(request: HttpRequest): HttpResponse {
        return map(
            request = request,
            decode = { it.readInt() },
            transform = {
                check(it in 1..1024) { "Number \"$it\" error!" }
                it * 2
            },
            encode = {
                val bytes = ByteArray(4)
                bytes.write(value = it)
                bytes
            },
        )
    }

    override fun route(request: HttpRequest): HttpResponse {
        logger.debug(
            message = """
                <-- request
                ${request.method} ${request.query}
                headers: ${request.headers}
            """.trimIndent(),
        )
        val response = when (val routes = mapping[request.query]) {
            null -> HttpResponse.NotFound()
            else -> when (val route = routes[request.method]) {
                null -> HttpResponse.MethodNotAllowed()
                else -> route(request)
            }
        }
        logger.debug(
            message = """
                --> response
                ${response.code} ${response.message}
                headers: ${response.headers}
            """.trimIndent(),
        )
        return response
    }
}
