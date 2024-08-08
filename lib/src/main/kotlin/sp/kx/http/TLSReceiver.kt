package sp.kx.http

import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import java.util.Date
import java.util.Objects
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class TLSReceiver(
    val secretKey: SecretKey,
    val id: UUID,
    val time: Duration,
    val encoded: ByteArray,
) {
    override fun toString(): String {
        return "{" +
            "secretKey: \"${secretKey.encoded.toHEX()}\", " +
            "id: $id, " +
            "time: ${Date(time.inWholeMilliseconds)}, " +
            "encoded: \"${encoded.toHEX()}\"" +
            "}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is TLSReceiver -> secretKey.encoded.contentEquals(other.secretKey.encoded) && id == other.id && time == other.time && encoded.contentEquals(other.encoded)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            secretKey.encoded.contentHashCode(),
            id,
            time,
            encoded.contentHashCode(),
        )
    }

    companion object {
        fun build(
            env: TLSEnvironment,
            methodCode: Byte,
            encodedQuery: ByteArray,
            body: ByteArray,
        ): TLSReceiver {
            val encryptedSK = ByteArray(body.readInt())
            System.arraycopy(body, 4, encryptedSK, 0, encryptedSK.size)
            val encrypted = ByteArray(body.readInt(index = 4 + encryptedSK.size))
            System.arraycopy(body, 4 + encryptedSK.size + 4, encrypted, 0, encrypted.size)
            val signature = ByteArray(body.readInt(index = 4 + encryptedSK.size + 4 + encrypted.size))
            System.arraycopy(body, 4 + encryptedSK.size + 4 + encrypted.size + 4, signature, 0, signature.size)
            val keyPair = env.getKeyPair()
            val secretKey = env.toSecretKey(env.decrypt(keyPair.private, encryptedSK))
            val payload = env.decrypt(secretKey, encrypted)
            val signatureData = ByteArray(payload.size + 1 + encodedQuery.size + secretKey.encoded.size)
            System.arraycopy(payload, 0, signatureData, 0, payload.size)
            signatureData[payload.size] = methodCode
            System.arraycopy(encodedQuery, 0, signatureData, payload.size + 1, encodedQuery.size)
            System.arraycopy(secretKey.encoded, 0, signatureData, payload.size + 1 + encodedQuery.size, secretKey.encoded.size)
            val verified = env.verify(keyPair.public, signatureData, signature = signature)
            if (!verified) error("Not verified!")
            val encoded = ByteArray(payload.readInt())
            val time = payload.readLong(index = 4 + encoded.size).milliseconds
            val id = payload.readUUID(index = 4 + encoded.size + 8)
            System.arraycopy(payload, 4, encoded, 0, encoded.size)
            return TLSReceiver(
                secretKey = secretKey,
                id = id,
                time = time,
                encoded = encoded,
            )
        }

        fun toHttpResponse(
            env: TLSEnvironment,
            secretKey: SecretKey,
            methodCode: Byte,
            encodedQuery: ByteArray,
            requestID: UUID,
            tlsResponse: TLSResponse,
        ): HttpResponse {
            val encodedSize = tlsResponse.encoded?.size ?: 0
            val payload = ByteArray(4 + encodedSize + 8)
            payload.write(value = encodedSize)
            if (tlsResponse.encoded != null && encodedSize > 0) {
                System.arraycopy(tlsResponse.encoded, 0, payload, 4, encodedSize)
            }
            payload.write(index = 4 + encodedSize, env.now().inWholeMilliseconds)
            val encrypted = env.encrypt(secretKey, payload)
            val encodedMessage = tlsResponse.message.toByteArray()
            val signatureData = ByteArray(payload.size + 16 + 1 + encodedQuery.size + 4 + encodedMessage.size)
            System.arraycopy(payload, 0, signatureData, 0, payload.size)
            signatureData.write(index = payload.size, requestID)
            signatureData[payload.size + 16] = methodCode
            System.arraycopy(encodedQuery, 0, signatureData, payload.size + 16 + 1, encodedQuery.size)
            signatureData.write(index = payload.size + 16 + 1 + encodedQuery.size, value = tlsResponse.code)
            System.arraycopy(encodedMessage, 0, signatureData, payload.size + 16 + 1 + encodedQuery.size + 4, encodedMessage.size)
            val keyPair = env.getKeyPair()
            val signature = env.sign(keyPair.private, signatureData)
            val body = ByteArray(4 + encrypted.size + 4 + signature.size)
            body.write(value = encrypted.size)
            System.arraycopy(encrypted, 0, body, 4, encrypted.size)
            body.write(index = 4 + encrypted.size, signature.size)
            System.arraycopy(signature, 0, body, 4 + encrypted.size + 4, signature.size)
            return HttpResponse(
                version = "1.1",
                code = tlsResponse.code,
                message = tlsResponse.message,
                headers = emptyMap(),
                body = body,
            )
        }
    }
}
