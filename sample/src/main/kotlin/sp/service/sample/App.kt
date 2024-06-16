package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sp.kx.http.HttpReceiver
import sp.service.sample.provider.FinalLoggers
import sp.service.sample.provider.FinalSecrets
import sp.service.sample.provider.Loggers
import sp.service.sample.provider.Secrets

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    val secrets: Secrets = FinalSecrets()
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        coroutineScope.launch {
            val routing = AppRouting(
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
