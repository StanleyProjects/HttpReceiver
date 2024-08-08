package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sp.kx.bytes.toHEX
import java.util.Objects
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class TLSTransmitterTest {
    @Test
    fun toStringTest() {
        val it = mockTLSTransmitter(
            secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56)),
            id = mockUUID(leastSigBits = 0x12ab),
            body = byteArrayOf(0x76, 0x54, 0x32, 0x10),
        )
        val expected = "{secretKey: \"123456\", id: 00000000-0000-0000-0000-0000000012ab, body: \"76543210\"}"
        val actual = it.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun hashCodeTest() {
        val secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56))
        val id = mockUUID(leastSigBits = 0x12ab)
        val body = byteArrayOf(0x76, 0x54, 0x32, 0x10)
        val it = mockTLSTransmitter(
            secretKey = secretKey,
            id = id,
            body = body,
        )
        val expected = Objects.hash(
            secretKey.encoded.contentHashCode(),
            id,
            body.contentHashCode(),
        )
        val actual = it.hashCode()
        assertEquals(expected, actual)
    }

    @Test
    fun equalsTest() {
        val secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56))
        val id = mockUUID(leastSigBits = 0x12ab)
        val body = byteArrayOf(0x76, 0x54, 0x32, 0x10)
        val it = mockTLSTransmitter(
            secretKey = secretKey,
            id = id,
            body = body,
        )
        val it1 = mockTLSTransmitter(
            secretKey = secretKey,
            id = id,
            body = body,
        )
        assertTrue(it == it1)
        listOf(
            mockTLSTransmitter(
                secretKey = MockSecretKey(encoded = byteArrayOf(0x54, 0x32, 0x10)),
                id = id,
                body = body,
            ),
            mockTLSTransmitter(
                secretKey = secretKey,
                id = mockUUID(leastSigBits = 0x34cd),
                body = body,
            ),
            mockTLSTransmitter(
                secretKey = secretKey,
                id = id,
                body = byteArrayOf(0x76, 0x54, 0x32, 0x10, 0x11),
            ),
        ).forEach { it2 ->
            assertFalse(it == it2)
        }
        assertNotEquals(it, Unit)
    }

    @Test
    fun buildTest() {
        val time = 12.milliseconds
        val id = mockUUID(13)
        val secretKey = MockSecretKey(encoded = mockByteArray(14))
        val encryptedSK = mockByteArray(15)
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(16)),
            publicKey = MockPublicKey(mockByteArray(17)),
        )
        val encoded = mockByteArray(18)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds) + toByteArray(id)
        val encrypted = mockByteArray(19)
        val methodCode: Byte = 21
        val encodedQuery = mockByteArray(22)
        val signatureData = payload + methodCode + encodedQuery + secretKey.encoded
        val signature = mockByteArray(23)
        val env = MockTLSEnvironment(
            keyPair = keyPair,
            timeProvider = { time },
            newSecretKeyProvider = { secretKey },
            newUUIDProvider = { id },
            items = listOf(
                Triple(keyPair.public.encoded, secretKey.encoded, encryptedSK),
                Triple(secretKey.encoded, payload, encrypted),
            ),
            signs = listOf(
                keyPair to (signatureData to signature),
            ),
        )
        val actual = TLSTransmitter.build(
            env = env,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            encoded = encoded,
        )
        val body = toByteArray(encryptedSK.size) + encryptedSK +
            toByteArray(encrypted.size) + encrypted +
            toByteArray(signature.size) + signature
        val expected = mockTLSTransmitter(
            secretKey = secretKey,
            id = id,
            body = body,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fromResponseTest() {
        val time = 12.milliseconds
        val timeNow = time + 1.seconds
        check(timeNow >= time)
        val timeMax = 1.minutes
        check(timeNow - time <= timeMax)
        val id = mockUUID(13)
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(14)),
            publicKey = MockPublicKey(mockByteArray(15)),
        )
        val secretKey = MockSecretKey(encoded = mockByteArray(16))
        val methodCode: Byte = 17
        val encodedQuery = mockByteArray(18)
        val encoded = mockByteArray(19)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds)
        val encrypted = mockByteArray(21)
        val responseCode = 1234
        val message = "TLSTransmitterTest:fromResponseTest:message"
        val signatureData = payload + toByteArray(id) + methodCode + encodedQuery + toByteArray(responseCode) + message.toByteArray()
        val signature = mockByteArray(22)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            keyPair = keyPair,
            timeProvider = { timeNow },
            items = listOf(
                Triple(secretKey.encoded, payload, encrypted),
            ),
            signs = listOf(
                keyPair to (signatureData to signature),
            ),
        )
        val body = toByteArray(encrypted.size) + encrypted + toByteArray(signature.size) + signature
        val actual = TLSTransmitter.fromResponse(
            env = env,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            secretKey = secretKey,
            requestID = id,
            responseCode = responseCode,
            message = message,
            body = body,
        )
        assertTrue(encoded.contentEquals(actual), "expected: ${encoded.toHEX()}, actual: ${actual.toHEX()}")
    }

    @Test
    fun fromResponseTimeIsUpTest() {
        val time = 12.milliseconds
        val timeMax = 1.minutes
        val timeNow = time + 1.seconds + timeMax
        check(timeNow >= time)
        check(timeNow - time >= timeMax)
        val secretKey = MockSecretKey(encoded = mockByteArray(16))
        val encoded = mockByteArray(19)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds)
        val encrypted = mockByteArray(21)
        val signature = mockByteArray(22)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            timeProvider = { timeNow },
            items = listOf(
                Triple(secretKey.encoded, payload, encrypted),
            ),
        )
        val body = toByteArray(encrypted.size) + encrypted + toByteArray(signature.size) + signature
        val throwable: IllegalStateException = assertThrows(IllegalStateException::class.java) {
            TLSTransmitter.fromResponse(
                env = env,
                methodCode = 17,
                encodedQuery = mockByteArray(18),
                secretKey = secretKey,
                requestID = mockUUID(),
                responseCode = -1,
                message = "",
                body = body,
            )
        }
        assertEquals("Time is up!", throwable.message)
    }

    @Test
    fun fromResponseVerifiedErrorTest() {
        val time = 12.milliseconds
        val timeNow = time + 1.seconds
        check(timeNow >= time)
        val timeMax = 1.minutes
        check(timeNow - time < timeMax)
        val id = mockUUID(13)
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(14)),
            publicKey = MockPublicKey(mockByteArray(15)),
        )
        val secretKey = MockSecretKey(encoded = mockByteArray(16))
        val methodCode: Byte = 17
        val encodedQuery = mockByteArray(18)
        val encoded = mockByteArray(19)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds)
        val encrypted = mockByteArray(21)
        val responseCode = 1234
        val message = "TLSTransmitterTest:fromResponseVerifiedErrorTest:message"
        val signatureData = payload + toByteArray(id) + methodCode + encodedQuery + toByteArray(responseCode) + message.toByteArray()
        val signature = mockByteArray(22)
        val signatureWrong = mockByteArray(23)
        check(!signature.contentEquals(signatureWrong))
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            keyPair = keyPair,
            timeProvider = { timeNow },
            items = listOf(
                Triple(secretKey.encoded, payload, encrypted),
            ),
            signs = listOf(
                keyPair to (signatureData to signatureWrong),
            ),
        )
        val body = toByteArray(encrypted.size) + encrypted + toByteArray(signature.size) + signature
        val throwable: IllegalStateException = assertThrows(IllegalStateException::class.java) {
            TLSTransmitter.fromResponse(
                env = env,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                secretKey = secretKey,
                requestID = id,
                responseCode = responseCode,
                message = message,
                body = body,
            )
        }
        assertEquals("Signature is invalid!", throwable.message)
    }

    @Test
    fun fromHttpResponseTest() {
        val time = 12.milliseconds
        val secretKey = MockSecretKey(encoded = byteArrayOf(0x0c, 0x03, 0xc, 0x09, 0x03, 0xd))
        val encryptedSK = mockByteArray(14)
        val id = mockUUID(0xdcba)
        val methodCode: Byte = 1
        val query = "foobarbaz"
        val encodedQuery = query.toByteArray()
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(byteArrayOf(0x0b, 0x09, 0x0e, 0x0b, 0x0a, 0x09, 0x03)),
            publicKey = MockPublicKey(byteArrayOf(0x0b, 0x07, 0x0b, 0x01, 0x0c)),
        )
        val responseEncoded = mockByteArray(12)
        val responsePayload = toByteArray(responseEncoded.size) + responseEncoded + toByteArray(time.inWholeMilliseconds)
        val responseEncrypted = mockByteArray(13)
        val responseCode = 200
        val message = "OK"
        val responseSignatureData = responsePayload + toByteArray(id) + methodCode + encodedQuery + toByteArray(responseCode) + message.toByteArray()
        val responseSignature = mockByteArray(18)
        val env = MockTLSEnvironment(
            keyPair = keyPair,
            timeProvider = { time },
            newSecretKeyProvider = { secretKey },
            items = listOf(
                Triple(keyPair.private.encoded, secretKey.encoded, encryptedSK),
                Triple(secretKey.encoded, responsePayload, responseEncrypted),
            ),
            keys = listOf(
                secretKey.encoded to secretKey,
            ),
            signs = listOf(
                keyPair to (responseSignatureData to responseSignature),
            ),
        )
        val tlsResponse = mockTLSResponse(
            code = responseCode,
            message = message,
            encoded = responseEncoded,
        )
        val httpResponse = TLSReceiver.toHttpResponse(
            env = env,
            secretKey = secretKey,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            requestID = id,
            tlsResponse = tlsResponse,
        )
        val actual = TLSTransmitter.fromResponse(
            env = env,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            secretKey = secretKey,
            requestID = id,
            responseCode = responseCode,
            message = message,
            body = httpResponse.body ?: error("No body!"),
        )
        assertTrue(responseEncoded.contentEquals(actual), "expected: ${responseEncoded.toHEX()}, actual: ${actual.toHEX()}")
    }
}
