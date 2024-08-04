package sp.service.transmitter.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import java.net.URL
import java.security.KeyPair
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

internal class FinalRemotes(
    loggers: Loggers,
    private val secrets: Secrets,
    private val keyPair: KeyPair,
    private val address: URL,
) : Remotes {
    private val logger = loggers.create("[Remotes]")
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun double(number: Int): Int {
        val requestTime = System.currentTimeMillis().milliseconds
        logger.debug("request time: ${Date(requestTime.inWholeMilliseconds)}") // todo
        val requestID = UUID.randomUUID()
        logger.debug("request ID: $requestID") // todo
        val encoded = ByteArray(4)
        encoded.write(value = number)
        val payload = ByteArray(4 + encoded.size + 8 + 16)
        payload.write(value = encoded.size)
        System.arraycopy(encoded, 0, payload, 4, encoded.size)
        payload.write(index = 4 + encoded.size, requestTime.inWholeMilliseconds)
        payload.write(index = 4 + encoded.size + 8, requestID)
        val query = "/double"
        logger.debug("payload: ${payload.toHEX()}") // todo
        logger.debug("query: \"$query\"") // todo
        val method = "POST"
        val methodCode: Byte = 1 // POST
        logger.debug("method: \"$method\"") // todo
        val secretKey = secrets.newSecretKey()
        logger.debug("secret:key:hash: ${secrets.hash(secretKey.encoded).toHEX()}") // todo
        val encryptedSK = secrets.encrypt(keyPair.public, secretKey.encoded)
        logger.debug("encrypted secret key: ${encryptedSK.toHEX()}") // todo
        val encrypted = secrets.encrypt(secretKey, payload)
        logger.debug("encrypted payload: ${encrypted.toHEX()}") // todo
        val encodedQuery = query.toByteArray()
        val signatureData = ByteArray(payload.size + 1 + encodedQuery.size + secretKey.encoded.size)
        System.arraycopy(payload, 0, signatureData, 0, payload.size)
        signatureData[payload.size] = methodCode
        System.arraycopy(encodedQuery, 0, signatureData, payload.size + 1, encodedQuery.size)
        System.arraycopy(secretKey.encoded, 0, signatureData, payload.size + 1 + encodedQuery.size, secretKey.encoded.size)
        logger.debug("signature:data: ${signatureData.toHEX()}") // todo
        logger.debug("signature:data:hash: ${secrets.hash(signatureData).toHEX()}") // todo
        val signature = secrets.sign(keyPair.private, signatureData)
        logger.debug("signature: ${signature.toHEX()}") // todo
        val requestBody = ByteArray(4 + encryptedSK.size + 4 + encrypted.size + 4 + signature.size)
        requestBody.write(value = encryptedSK.size)
        System.arraycopy(encryptedSK, 0, requestBody, 4, encryptedSK.size)
        requestBody.write(index = 4 + encryptedSK.size, encrypted.size)
        System.arraycopy(encrypted, 0, requestBody, 4 + encryptedSK.size + 4, encrypted.size)
        requestBody.write(index = 4 + encryptedSK.size + 4 + encrypted.size, signature.size)
        System.arraycopy(signature, 0, requestBody, 4 + encryptedSK.size + 4 + encrypted.size + 4, signature.size)
        return client.newCall(
            request = Request.Builder()
                .url(URL(address, query))
                .method(method, requestBody.toRequestBody())
                .build(),
        ).execute().use { response ->
            when (response.code) {
                200 -> {
                    TODO("FinalRemotes:double($number)")
                }
                else -> error("Unknown code: ${response.code}!")
            }
        }
    }
}
