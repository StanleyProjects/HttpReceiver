package sp.service.sample

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import sp.kx.http.HttpRequest
import sp.kx.http.HttpResponse
import sp.kx.http.HttpRouting
import sp.service.sample.provider.Loggers
import sp.service.sample.provider.Secrets
import javax.crypto.SecretKey

internal class AppRouting(
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
        "/double" to mapOf(
            "POST" to ::onPostDouble,
        ),
    )

    private var secretKey: SecretKey? = null

    private fun onPostDouble(request: HttpRequest): HttpResponse {
        logger.debug("on post double...")
        val body = request.body ?: return HttpResponse(
            version = "1.1",
            code = 500,
            message = "Internal Server Error",
            headers = mapOf(
                "message" to "No body!",
            ),
            body = "todo".toByteArray(),
        )
        logger.debug("encrypted:request: ${secrets.hash(body)}")
        val secretKey = secretKey ?: return HttpResponse(
            version = "1.1",
            code = 500,
            message = "Internal Server Error",
            headers = mapOf(
                "message" to "No secret key!",
            ),
            body = "todo".toByteArray(),
        )
        val decryptedRequest = secrets.decrypt(secretKey = secretKey, body)
        logger.debug("decrypted:request: ${secrets.hash(decryptedRequest)}")
        val number = try {
            String(decryptedRequest).toInt().also { check(it in 1..128) }
        } catch (e: Throwable) {
            return HttpResponse(
                version = "1.1",
                code = 500,
                message = "Internal Server Error",
                headers = mapOf(
                    "message" to "Wrong number!",
                ),
                body = "todo".toByteArray(),
            )
        }
        val decryptedResponse = "${number * 2}".toByteArray()
        logger.debug("decrypted:response: ${secrets.hash(decryptedResponse)}")
        val encryptedResponse = secrets.encrypt(secretKey = secretKey, decryptedResponse)
        logger.debug("encrypted:response: ${secrets.hash(encryptedResponse)}")
        this.secretKey = null
        return HttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = encryptedResponse,
        )
    }

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
        val publicKey = secrets.toPublicKey(body)
        logger.debug("public:key: ${secrets.hash(publicKey.encoded)}")
        val secretKey = secrets.newSecretKey()
        logger.debug("secret:key: ${secrets.hash(secretKey.encoded)}")
        val encrypted = secrets.encrypt(publicKey, secretKey.encoded)
        logger.debug("secret:key:encrypted: ${secrets.hash(encrypted)}")
        this.secretKey = secretKey
        return HttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = encrypted,
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
        runBlocking {
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
