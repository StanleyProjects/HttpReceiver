package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sp.kx.http.HttpReceiver
import sp.kx.secrets.Asymmetric
import sp.kx.secrets.Symmetric
import sp.kx.tlsmessages.RealTLSReceiver
import sp.service.sample.provider.FinalLoggers
import sp.service.sample.provider.Loggers
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.util.UUID
import kotlin.time.Duration

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    val keyStore = KeyStore.getInstance("PKCS12")
    val alias = "a202"
    val password = "qwe202"
    Thread.currentThread().contextClassLoader.getResourceAsStream("a202.pkcs12").use {
        if (it == null) error("No stream!")
        logger.debug("load key store...")
        keyStore.load(it, password.toCharArray())
    }
    val key = keyStore.getKey(alias, password.toCharArray()) ?: error("No \"$alias\"!")
    check(key is PrivateKey)
    val certificate = keyStore.getCertificate(alias)
    val keyPair = KeyPair(certificate.publicKey, key)
    val requested = mutableMapOf<UUID, Duration>()
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        coroutineScope.launch {
            val routing = AppRouting(
                loggers = loggers,
                receiver = RealTLSReceiver(
                    requested = requested,
                    symmetric = Symmetric.AES,
                    asymmetric = Asymmetric.RSA,
                    keyPair = keyPair,
                ),
                coroutineScope = coroutineScope,
            )
            val receiver = HttpReceiver(routing)
            launch {
                routing.events.collect { event ->
                    logger.debug("event: $event")
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
