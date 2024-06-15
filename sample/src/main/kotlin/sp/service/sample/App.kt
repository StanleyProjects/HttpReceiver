package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sp.kx.http.HttpReceiver
import sp.kx.http.HttpRequest
import sp.kx.http.HttpResponse
import sp.kx.http.HttpRouting
import sp.service.sample.provider.FinalLoggers
import sp.service.sample.provider.FinalSecrets
import sp.service.sample.provider.Loggers
import sp.service.sample.provider.Secrets

private class AppRouting(
    private val coroutineScope: CoroutineScope,
    private val version: Int,
    loggers: Loggers,
    private val secrets: Secrets,
) : HttpRouting {
    sealed interface Event {
        data object Quit : Event
    }

    private val logger = loggers.create("[Routing]")
    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val mapping = mapOf(
        "/version" to mapOf(
            "GET" to ::onGetVersion,
        ),
        "/quit" to mapOf(
            "GET" to ::onGetQuit,
        ),
        "/session/start" to mapOf(
            "POST" to ::onPostSessionStart,
        ),
    )

    private fun onPostSessionStart(request: HttpRequest): HttpResponse {
        logger.debug("on post session start...")
        val body = request.body ?: return HttpResponse(
            version = "1.1",
            code = 500,
            message = "Internal Server Error",
            headers = mapOf(
                "message" to "No body!",
            ),
            body = "todo".toByteArray(),
        )
        logger.debug("body(${body.size}): ${secrets.hash(body)}")
        val publicKey = secrets.toPublicKey(body)
        logger.debug("public:key: ${secrets.hash(publicKey.encoded)}")
        return HttpResponse(
            version = "1.1",
            code = 500,
            message = "Internal Server Error",
            headers = emptyMap(),
            body = "todo".toByteArray(),
        )
    }

    private fun onGetVersion(request: HttpRequest): HttpResponse {
        return HttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = "$version".toByteArray(),
        )
    }

    private fun onGetQuit(request: HttpRequest): HttpResponse {
        coroutineScope.launch {
            _events.emit(Event.Quit)
        }
        return HttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = null,
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
        val response = when (val route = mapping[request.query]) {
            null -> HttpResponse(
                version = "1.1",
                code = 404,
                message = "Not Found",
                headers = emptyMap(),
                body = null,
            )
            else -> when (val transform = route[request.method]) {
                null -> HttpResponse(
                    version = "1.1",
                    code = 405,
                    message = "Method Not Allowed",
                    headers = emptyMap(),
                    body = null,
                )
                else -> transform(request)
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

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    val secrets: Secrets = FinalSecrets()
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        coroutineScope.launch {
            val routing = AppRouting(
                coroutineScope = this,
                version = 2,
                loggers = loggers,
                secrets = secrets,
            )
            val receiver = HttpReceiver(routing)
            launch {
                routing.events.collect { event ->
                    when (event) {
                        AppRouting.Event.Quit -> receiver.stop()
                    }
                }
            }
            launch {
                receiver.states.collect { state ->
                    logger.debug("state: $state")
                    when (state) {
                        is HttpReceiver.State.Started -> {
                            if (!state.stopping) {
                                logger.debug("started: ${state.host}:${state.port}")
                            }
                        }
                        is HttpReceiver.State.Stopped -> {
                            if (!state.starting) {
                                logger.debug("stopped")
                                job.cancel()
                            }
                        }
                    }
                }
            }
            receiver.start(port = 40631)
        }.join()
    }
}
