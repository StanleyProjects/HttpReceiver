package sp.service.transmitter.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sp.kx.bytes.readInt
import sp.kx.bytes.write
import sp.kx.http.TLSEnvironment
import sp.kx.http.TLSTransmitter
import java.net.URL
import java.security.KeyPair
import java.util.concurrent.TimeUnit

internal class FinalRemotes(
    loggers: Loggers,
    private val tlsEnv: TLSEnvironment,
    private val address: URL,
) : Remotes {
    private val logger = loggers.create("[Remotes]")
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun <T : Any> map(
        method: String,
        query: String,
        encoded: ByteArray,
        decode: (ByteArray) -> T,
    ): T {
        logger.debug("method: \"$method\"") // todo
        val methodCode: Byte = TLSEnvironment.getMethodCode(method = method)
        logger.debug("query: \"$query\"") // todo
        val encodedQuery = query.toByteArray()
        val tlsTransmitter = TLSTransmitter.build(
            env = tlsEnv,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            encoded = encoded,
        )
        return client.newCall(
            request = Request.Builder()
                .url(URL(address, query))
                .method(method, tlsTransmitter.body.toRequestBody())
                .build(),
        ).execute().use { response ->
            when (response.code) {
                200 -> {
                    val responseEncoded = TLSTransmitter.fromResponse(
                        env = tlsEnv,
                        methodCode = methodCode,
                        encodedQuery = encodedQuery,
                        secretKey = tlsTransmitter.secretKey,
                        requestID = tlsTransmitter.id,
                        responseCode = response.code,
                        message = response.message,
                        body = response.body?.bytes() ?: error("No body!"),
                    )
                    decode(responseEncoded)
                }
                else -> error("Unknown code: ${response.code}!")
            }
        }
    }

    override fun double(number: Int): Int {
        val bytes = ByteArray(4)
        bytes.write(value = number)
        return map(
            method = "POST",
            query = "/double",
            encoded = bytes,
            decode = { it.readInt() },
        )
    }
}
