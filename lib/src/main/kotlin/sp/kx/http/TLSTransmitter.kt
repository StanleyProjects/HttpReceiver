package sp.kx.http

import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import java.security.KeyPair
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class TLSTransmitter private constructor(
    val secretKey: SecretKey,
    val id: UUID,
    val body: ByteArray,
) {
    override fun toString(): String {
        return "{" +
            "secretKey: ${secretKey.encoded.toHEX()}, " +
            "id: $id, " +
            "body: ${body.toHEX()}, " +
            "}"
    }

    companion object {
        fun build(
            env: TLSEnvironment,
            keyPair: KeyPair,
            methodCode: Byte,
            encodedQuery: ByteArray,
            encoded: ByteArray,
            id: UUID,
        ): TLSTransmitter {
            val payload = ByteArray(4 + encoded.size + 8 + 16)
            payload.write(value = encoded.size)
            System.arraycopy(encoded, 0, payload, 4, encoded.size)
            payload.write(index = 4 + encoded.size, env.now().inWholeMilliseconds)
            payload.write(index = 4 + encoded.size + 8, id)
            val secretKey = env.newSecretKey()
            val encryptedSK = env.encrypt(keyPair.public, secretKey.encoded)
            val encrypted = env.encrypt(secretKey, payload)
            val signatureData = ByteArray(payload.size + 1 + encodedQuery.size + secretKey.encoded.size)
            System.arraycopy(payload, 0, signatureData, 0, payload.size)
            signatureData[payload.size] = methodCode
            System.arraycopy(encodedQuery, 0, signatureData, payload.size + 1, encodedQuery.size)
            System.arraycopy(secretKey.encoded, 0, signatureData, payload.size + 1 + encodedQuery.size, secretKey.encoded.size)
            val signature = env.sign(keyPair.private, signatureData)
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

        fun fromResponse(
            env: TLSEnvironment,
            keyPair: KeyPair,
            methodCode: Byte,
            encodedQuery: ByteArray,
            secretKey: SecretKey,
            requestID: UUID,
            body: ByteArray,
        ): ByteArray {
            val encryptedPayload = ByteArray(body.readInt())
            System.arraycopy(body, 4, encryptedPayload, 0, encryptedPayload.size)
            val payload = env.decrypt(secretKey, encryptedPayload)
            val encoded = ByteArray(payload.readInt())
            val time = payload.readLong(index = 4 + encoded.size).milliseconds
            val now = env.now()
            // todo now < requestTime
            val maxTime = env.maxTime
            if (now - time > maxTime) TODO("FinalRemotes:onDouble:time")
            val signature = ByteArray(body.readInt(index = 4 + encryptedPayload.size))
            System.arraycopy(body, 4 + encryptedPayload.size + 4, signature, 0, signature.size)
            val signatureData = ByteArray(payload.size + 16 + 1 + encodedQuery.size)
            System.arraycopy(payload, 0, signatureData, 0, payload.size)
            signatureData.write(index = payload.size, requestID)
            signatureData[payload.size + 16] = methodCode
            System.arraycopy(encodedQuery, 0, signatureData, payload.size + 16 + 1, encodedQuery.size)
            val verified = env.verify(keyPair.public, signatureData, signature)
            if (!verified) TODO("FinalRemotes:onDouble:!verified")
            System.arraycopy(payload, 4, encoded, 0, encoded.size)
            return encoded
        }
    }
}
