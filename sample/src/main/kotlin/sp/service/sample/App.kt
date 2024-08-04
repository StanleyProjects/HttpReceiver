package sp.service.sample

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import sp.kx.bytes.toHEX
import sp.kx.http.HttpReceiver
import sp.service.sample.provider.FinalLoggers
import sp.service.sample.provider.FinalSecrets
import sp.service.sample.provider.Loggers
import sp.service.sample.provider.Secrets
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    val secrets: Secrets = FinalSecrets()
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
    logger.debug("private:key:hash: ${secrets.hash(key.encoded).toHEX()}")
    val certificate = keyStore.getCertificate(alias)
    logger.debug("public:key:hash: ${secrets.hash(certificate.publicKey.encoded).toHEX()}")
    val keyPair = KeyPair(certificate.publicKey, key)
    runBlocking {
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        coroutineScope.launch {
            val routing = AppRouting(
                loggers = loggers,
                secrets = secrets,
                keyPair = keyPair,
                requested = mutableMapOf(),
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
