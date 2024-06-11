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

private class AppRouting(
    private val coroutineScope: CoroutineScope,
    private val version: String,
) : HttpRouting {
    sealed interface Event {
        data object Quit : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val mapping = mapOf(
        "/version" to mapOf(
            "GET" to ::onGetVersion,
        ),
        "/quit" to mapOf(
            "GET" to ::onGetQuit,
        ),
    )

    private fun onGetVersion(request: HttpRequest): HttpResponse {
        return HttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = version.toByteArray(),
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
        val route = mapping[request.query] ?: return HttpResponse(
            version = "1.1",
            code = 404,
            message = "Not Found",
            headers = emptyMap(),
            body = null,
        )
        val transform = route[request.method] ?: return HttpResponse(
            version = "1.1",
            code = 405,
            message = "Method Not Allowed",
            headers = emptyMap(),
            body = null,
        )
        return transform(request)
    }
}

fun main() {
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        coroutineScope.launch {
            val routing = AppRouting(
                coroutineScope = this,
                version = "0.0.1",
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
                    println("state: $state")
                    when (state) {
                        is HttpReceiver.State.Started -> {
                            if (!state.stopping) {
                                println("started: ${state.host}:${state.port}")
                            }
                        }
                        is HttpReceiver.State.Stopped -> {
                            if (!state.starting) {
                                println("stopped")
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
