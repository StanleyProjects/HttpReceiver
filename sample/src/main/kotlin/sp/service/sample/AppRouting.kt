package sp.service.sample

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import sp.kx.bytes.readInt
import sp.kx.bytes.write
import sp.kx.http.HttpRequest
import sp.kx.http.HttpResponse
import sp.kx.http.TLSRouting
import sp.service.sample.provider.Loggers
import sp.service.sample.provider.Secrets
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class AppRouting(
    loggers: Loggers,
    private val secrets: Secrets,
    private val keyPair: KeyPair,
    override var requested: Map<UUID, Duration>,
) : TLSRouting() {
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
        runBlocking {
            _events.emit(Event.Quit)
        }
        return HttpResponse.OK()
    }

    private fun onPostDouble(request: HttpRequest): HttpResponse {
        return map(
            request = request,
            decode = { it.readInt() },
            transform = {
                check(it in 1..1024)
                it * 2
            },
            encode = {
                val bytes = ByteArray(4)
                bytes.write(value = it)
                bytes
            },
        )
    }

    override fun getKeyPair(): KeyPair {
        return keyPair
    }

    override fun toSecretKey(encoded: ByteArray): SecretKey {
        TODO("AppRouting:toSecretKey")
    }

    override fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray {
        return secrets.decrypt(key, encrypted)
    }

    override fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        return secrets.decrypt(key, encrypted)
    }

    override fun encrypt(key: SecretKey, encrypted: ByteArray): ByteArray {
        return secrets.encrypt(key, encrypted)
    }

    override fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean {
        return secrets.verify(key, encoded, signature)
    }

    override fun sign(key: PrivateKey, encoded: ByteArray): ByteArray {
        return secrets.sign(key, encoded)
    }

    override fun getMaxTime(): Duration {
        return 1.minutes
    }

    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
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
