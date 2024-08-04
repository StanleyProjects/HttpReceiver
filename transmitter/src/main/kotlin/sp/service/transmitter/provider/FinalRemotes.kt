package sp.service.transmitter.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import sp.kx.http.TLSTransmitter
import java.net.URL
import java.security.KeyPair
import java.security.PublicKey
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

    private fun onResponse(
        secretKey: SecretKey,
        publicKey: PublicKey,
        responseBody: ByteArray,
        methodCode: Byte,
        encodedQuery: ByteArray,
        requestID: UUID,
    ): ByteArray {
        val encryptedPayload = ByteArray(responseBody.readInt())
        System.arraycopy(responseBody, 4, encryptedPayload, 0, encryptedPayload.size)
        logger.debug("response encrypted payload: ${encryptedPayload.toHEX()}") // todo
        val payload = secrets.decrypt(secretKey, encryptedPayload)
        logger.debug("response payload: ${payload.toHEX()}") // todo
        val responseEncoded = ByteArray(payload.readInt())
        System.arraycopy(payload, 4, responseEncoded, 0, responseEncoded.size)
        logger.debug("response encoded: ${responseEncoded.toHEX()}") // todo
        val responseTime = payload.readLong(index = 4 + responseEncoded.size).milliseconds
        logger.debug("response time: ${Date(responseTime.inWholeMilliseconds)}") // todo
        val signature = ByteArray(responseBody.readInt(index = 4 + encryptedPayload.size))
        System.arraycopy(responseBody, 4 + encryptedPayload.size + 4, signature, 0, signature.size)
        logger.debug("response:signature: ${signature.toHEX()}") // todo
        logger.debug("response:signature:hash: ${secrets.hash(signature).toHEX()}") // todo
        val signatureData = ByteArray(payload.size + 16 + 1 + encodedQuery.size)
        System.arraycopy(payload, 0, signatureData, 0, payload.size)
        signatureData.write(index = payload.size, requestID)
        signatureData[payload.size + 16] = methodCode
        System.arraycopy(encodedQuery, 0, signatureData, payload.size + 16 + 1, encodedQuery.size)
        logger.debug("response:signature:data: ${signatureData.toHEX()}") // todo
        logger.debug("response:signature:data:hash: ${secrets.hash(signatureData).toHEX()}") // todo
        val verified = secrets.verify(publicKey, signatureData, signature)
        if (!verified) TODO("FinalRemotes:onDouble:!verified")
        val now = System.currentTimeMillis().milliseconds
        // todo now < requestTime
        val maxTime = 1.minutes
        if (now - responseTime > maxTime) TODO("FinalRemotes:onDouble:time")
        return responseEncoded
    }

    private fun toTransmitter(
        encoded: ByteArray,
        methodCode: Byte,
        encodedQuery: ByteArray,
    ): TLSTransmitter {
        val requestTime = System.currentTimeMillis()
        logger.debug("request time: ${Date(requestTime)}") // todo
        val id = UUID.randomUUID()
        logger.debug("request ID: $id") // todo
        val payload = ByteArray(4 + encoded.size + 8 + 16)
        payload.write(value = encoded.size)
        System.arraycopy(encoded, 0, payload, 4, encoded.size)
        payload.write(index = 4 + encoded.size, requestTime)
        payload.write(index = 4 + encoded.size + 8, id)
        val query = "/double"
        logger.debug("payload: ${payload.toHEX()}") // todo
        logger.debug("query: \"$query\"") // todo
        val secretKey = secrets.newSecretKey()
        logger.debug("secret:key:hash: ${secrets.hash(secretKey.encoded).toHEX()}") // todo
        val encryptedSK = secrets.encrypt(keyPair.public, secretKey.encoded)
        logger.debug("encrypted secret key: ${encryptedSK.toHEX()}") // todo
        val encrypted = secrets.encrypt(secretKey, payload)
        logger.debug("encrypted payload: ${encrypted.toHEX()}") // todo
        val signatureData = ByteArray(payload.size + 1 + encodedQuery.size + secretKey.encoded.size)
        System.arraycopy(payload, 0, signatureData, 0, payload.size)
        signatureData[payload.size] = methodCode
        System.arraycopy(encodedQuery, 0, signatureData, payload.size + 1, encodedQuery.size)
        System.arraycopy(secretKey.encoded, 0, signatureData, payload.size + 1 + encodedQuery.size, secretKey.encoded.size)
        logger.debug("signature:data: ${signatureData.toHEX()}") // todo
        logger.debug("signature:data:hash: ${secrets.hash(signatureData).toHEX()}") // todo
        val signature = secrets.sign(keyPair.private, signatureData)
        logger.debug("signature: ${signature.toHEX()}") // todo
        val body = ByteArray(4 + encryptedSK.size + 4 + encrypted.size + 4 + signature.size)
        body.write(value = encryptedSK.size)
        System.arraycopy(encryptedSK, 0, body, 4, encryptedSK.size)
        body.write(index = 4 + encryptedSK.size, encrypted.size)
        System.arraycopy(encrypted, 0, body, 4 + encryptedSK.size + 4, encrypted.size)
        body.write(index = 4 + encryptedSK.size + 4 + encrypted.size, signature.size)
        System.arraycopy(signature, 0, body, 4 + encryptedSK.size + 4 + encrypted.size + 4, signature.size)
        return TLSTransmitter(
            secretKey = secretKey,
            id = id,
            body = body,
        )
    }

    override fun double(number: Int): Int {
        val query = "/double"
        logger.debug("query: \"$query\"") // todo
        val encodedQuery = query.toByteArray()
        val method = "POST"
        val methodCode: Byte = 1 // POST
        logger.debug("method: \"$method\"") // todo
        val encoded = ByteArray(4)
        encoded.write(value = number)
        val tlsTransmitter = toTransmitter(
            encoded = encoded,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
        )
        return client.newCall(
            request = Request.Builder()
                .url(URL(address, query))
                .method(method, tlsTransmitter.body.toRequestBody())
                .build(),
        ).execute().use { response ->
            when (response.code) {
                200 -> {
                    val responseBody = response.body?.bytes() ?: error("No body!")
                    val responseEncoded = onResponse(
                        secretKey = tlsTransmitter.secretKey,
                        publicKey = keyPair.public,
                        responseBody = responseBody,
                        encodedQuery = encodedQuery,
                        methodCode = methodCode,
                        requestID = tlsTransmitter.id,
                    )
                    responseEncoded.readInt()
                }
                else -> error("Unknown code: ${response.code}!")
            }
        }
    }
}
