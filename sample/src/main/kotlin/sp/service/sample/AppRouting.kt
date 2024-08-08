package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sp.kx.bytes.readInt
import sp.kx.bytes.write
import sp.kx.http.HttpRequest
import sp.kx.http.HttpResponse
import sp.kx.http.TLSEnvironment
import sp.kx.http.TLSResponse
import sp.kx.http.TLSRouting
import sp.service.sample.provider.Loggers
import java.util.UUID
import kotlin.time.Duration

internal class AppRouting(
    loggers: Loggers,
    tlsEnv: TLSEnvironment,
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
            withContext(Dispatchers.Default) {
                _events.emit(Event.Quit)
            }
        }
        return HttpResponse.OK()
    }

    private fun onPostDouble(request: HttpRequest): HttpResponse {
        return map(
            request = request,
            transform = { encoded ->
                val number = encoded.readInt()
                check(number in 1..1024) { "Number \"$number\" error!" }
                val bytes = ByteArray(4)
                bytes.write(value = number * 2)
                TLSResponse.OK(encoded = bytes)
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
