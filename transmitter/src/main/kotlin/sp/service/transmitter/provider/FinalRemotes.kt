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
    private val keyPair: KeyPair,
    private val address: URL,
) : Remotes {
    private val logger = loggers.create("[Remotes]")
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun <REQ : Any, RES : Any> map(
        method: String,
        query: String,
        decoded: REQ,
        encode: (REQ) -> ByteArray,
        decode: (ByteArray) -> RES,
    ): RES {
        logger.debug("method: \"$method\"") // todo
        val methodCode: Byte = TLSEnvironment.getMethodCode(method = method)
        logger.debug("query: \"$query\"") // todo
        val encodedQuery = query.toByteArray()
        val requestEncoded = encode(decoded)
        val tlsTransmitter = TLSTransmitter.build(
            env = tlsEnv,
            keyPair = keyPair,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            encoded = requestEncoded,
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
                        keyPair = keyPair,
                        methodCode = methodCode,
                        encodedQuery = encodedQuery,
                        secretKey = tlsTransmitter.secretKey,
                        body = response.body?.bytes() ?: error("No body!"),
                        requestID = tlsTransmitter.id,
                    )
                    decode(responseEncoded)
                }
                else -> error("Unknown code: ${response.code}!")
            }
        }
    }

    override fun double(number: Int): Int {
        return map(
            method = "POST",
            query = "/double",
            decoded = number,
            encode = {
                val bytes = ByteArray(4)
                bytes.write(value = it)
                bytes
            },
            decode = { it.readInt() }
        )
    }
}
