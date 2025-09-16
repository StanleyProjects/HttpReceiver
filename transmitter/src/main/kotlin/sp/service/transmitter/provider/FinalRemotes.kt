package sp.service.transmitter.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sp.kx.bytes.readInt
import sp.kx.bytes.write
import sp.kx.tlsmessages.TLSTransmitter
import java.net.URL
import java.util.concurrent.TimeUnit

internal class FinalRemotes(
    loggers: Loggers,
    private val transmitter: TLSTransmitter,
    private val address: URL,
) : Remotes {
    private val logger = loggers.create("[Remotes]")
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun <T : Any> map(
        method: String,
        query: String,
        body: ByteArray,
        decode: (ByteArray) -> T,
    ): T {
        logger.debug("method: \"$method\"") // todo
        logger.debug("query: \"$query\"") // todo
        val request = transmitter.toRequest(
            method = method,
            query = query,
            body = body,
        )
        return client.newCall(
            request = Request.Builder()
                .url(URL(address, query))
                .method(method, request.bytes.toRequestBody())
                .build(),
        ).execute().use { response ->
            when (response.code) {
                200 -> {
                    val responseEncoded = transmitter.fromResponseBody(
                        code = response.code,
                        message = response.message,
                        issuer = request.issuer,
                        bytes = response.body?.bytes() ?: error("No body!"),
                    )
                    decode(responseEncoded)
                }
                else -> error("Unknown code: ${response.code}!")
            }
        }
    }

    override fun double(number: Int): Int {
        val body = ByteArray(4)
        body.write(value = number)
        return map(
            method = "POST",
            query = "/double",
            body = body,
            decode = { it.readInt() },
        )
    }
}
