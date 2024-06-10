package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import sp.kx.http.HttpReceiver
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        val stopped = AtomicBoolean(true)
        coroutineScope.launch {
            val receiver = HttpReceiver()
            launch {
                receiver.state.collect { state ->
                    println("state: $state")
                    when (state) {
                        is HttpReceiver.State.Started -> {
                            if (!state.stopping) {
                                println("started: ${state.host}:${state.port}")
                                stopped.set(false)
                            }
                        }
                        is HttpReceiver.State.Stopped -> {
                            if (!state.starting && stopped.compareAndSet(false, true)) {
                                println("stopped")
                                job.cancel()
                            }
                        }
                    }
                }
            }
            launch(Dispatchers.Default) {
                receiver.start()
            }
            withContext(Dispatchers.Default) {
                delay(5_000)
            }
            receiver.stop()
        }.join()
    }
}
