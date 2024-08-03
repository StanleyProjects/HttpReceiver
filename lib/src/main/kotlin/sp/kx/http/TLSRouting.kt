package sp.kx.http

import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.write
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class TLSRouting : HttpRouting {
    abstract fun getKeyPair(): KeyPair
    abstract fun toSecretKey(encoded: ByteArray): SecretKey
    abstract fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray
    abstract fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    abstract fun encrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    abstract fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean
    abstract fun sign(key: PrivateKey, encoded: ByteArray): ByteArray
    abstract fun getMaxTime(): Duration
    abstract fun now(): Duration
    abstract var requested: Map<UUID, Duration>

    private fun toResponse(
        secretKey: SecretKey,
        privateKey: PrivateKey,
        methodCode: Byte,
        encodedQuery: ByteArray,
        requestID: UUID,
        encoded: ByteArray,
    ): HttpResponse {
        val payload = ByteArray(4 + encoded.size + 8)
        payload.write(value = encoded.size)
        System.arraycopy(encoded, 0, payload, 4, encoded.size)
        payload.write(index = 4 + encoded.size, now().inWholeMilliseconds)
        val encrypted = encrypt(secretKey, payload)
        val signatureData = ByteArray(payload.size + 16 + 1 + encodedQuery.size)
        System.arraycopy(payload, 0, signatureData, 0, payload.size)
        signatureData.write(index = payload.size, requestID)
        signatureData[payload.size + 16] = methodCode
        System.arraycopy(encodedQuery, 0, signatureData, payload.size + 16 + 1, encodedQuery.size)
        val signature = sign(privateKey, signatureData)
        val body = ByteArray(4 + encrypted.size + 4 + signature.size)
        body.write(value = encrypted.size)
        System.arraycopy(encrypted, 0, body, 4, encrypted.size)
        body.write(index = 4 + encrypted.size, signature.size)
        System.arraycopy(signature, 0, body, 4 + encrypted.size + 4, signature.size)
        return HttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = body,
        )
    }

    private fun <T : Any> toRequest(
        keyPair: KeyPair,
        body: ByteArray,
        methodCode: Byte,
        encodedQuery: ByteArray,
        transform: (ByteArray) -> T,
    ): TLSRequest<T> {
        val encryptedSK = ByteArray(body.readInt())
        System.arraycopy(body, 4, encryptedSK, 0, encryptedSK.size)
        val encrypted = ByteArray(body.readInt(index = 4 + encryptedSK.size))
        System.arraycopy(body, 4 + encryptedSK.size + 4, encrypted, 0, encrypted.size)
        val signature = ByteArray(body.readInt(index = 4 + encryptedSK.size + 4 + encrypted.size))
        System.arraycopy(body, 4 + encryptedSK.size + 4 + encrypted.size + 4, signature, 0, signature.size)
        val secretKey = toSecretKey(decrypt(keyPair.private, encryptedSK))
        val payload = decrypt(secretKey, encrypted)
        val signatureData = ByteArray(payload.size + 1 + encodedQuery.size + secretKey.encoded.size)
        System.arraycopy(payload, 0, signatureData, 0, payload.size)
        signatureData[payload.size] = methodCode
        System.arraycopy(encodedQuery, 0, signatureData, payload.size + 1, encodedQuery.size)
        System.arraycopy(secretKey.encoded, 0, signatureData, payload.size + 1 + encodedQuery.size, secretKey.encoded.size)
        val verified = verify(keyPair.public, signatureData, signature = signature)
        if (!verified) TODO()
        val encoded = ByteArray(payload.readInt())
        val time = payload.readLong(index = 4 + encoded.size).milliseconds
        val now = now()
        // todo now < time
        val maxTime = getMaxTime()
        if (now - time > maxTime) TODO()
        val id = payload.readUUID(index = 4 + encoded.size + 8)
        val requested = requested
        if (requested.containsKey(id)) {
            TODO()
        } else if (requested.any { (_, it) -> now - it > maxTime }) {
            this.requested = requested.filterValues { now - it > maxTime}
        }
        this.requested += id to time
        return TLSRequest(
            secretKey = secretKey,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            id = id,
            decoded = transform(encoded),
        )
    }

    fun <REQ : Any, RES : Any> map(
        request: HttpRequest,
        decode: (ByteArray) -> REQ,
        transform: (REQ) -> RES,
        encode: (RES) -> ByteArray,
    ): HttpResponse {
        val body = request.body ?: return HttpResponse(
            version = "1.1",
            code = 500,
            message = "Internal Server Error",
            headers = mapOf("message" to "No body!"),
            body = "todo".toByteArray(),
        )
        val keyPair = getKeyPair()
        val tlsRequest = try {
            val methodCode = getMethodCode(request = request)
            val encodedQuery = request.query.toByteArray()
            toRequest(
                keyPair = keyPair,
                body = body,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                transform = decode,
            )
        } catch (_: Throwable) {
            return HttpResponse(
                version = "1.1",
                code = 500,
                message = "Internal Server Error",
                headers = emptyMap(),
                body = "todo".toByteArray(),
            )
        }
        val encoded = try {
            encode(transform(tlsRequest.decoded))
        } catch (_: Throwable) {
            return HttpResponse(
                version = "1.1",
                code = 500,
                message = "Internal Server Error",
                headers = emptyMap(),
                body = "todo".toByteArray(),
            )
        }
        return toResponse(
            secretKey = tlsRequest.secretKey,
            privateKey = keyPair.private,
            methodCode = tlsRequest.methodCode,
            encodedQuery = tlsRequest.encodedQuery,
            requestID = tlsRequest.id,
            encoded = encoded,
        )
    }

    companion object {
        private fun getMethodCode(request: HttpRequest): Byte {
            return when (request.method) {
                "POST" -> 1
                else -> error("Method \"${request.method}\" is not supported!")
            }
        }
    }
}
