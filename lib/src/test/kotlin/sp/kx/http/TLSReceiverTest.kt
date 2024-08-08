package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.Objects
import kotlin.time.Duration.Companion.milliseconds

internal class TLSReceiverTest {
    @Test
    fun toStringTest() {
        val time = 123.milliseconds
        val it = mockTLSReceiver(
            secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56)),
            id = mockUUID(leastSigBits = 0x12ab),
            time = time,
            encoded = byteArrayOf(0x76, 0x54, 0x32, 0x10),
        )
        val expected = "{secretKey: \"123456\", id: 00000000-0000-0000-0000-0000000012ab, time: ${Date(time.inWholeMilliseconds)}, encoded: \"76543210\"}"
        val actual = it.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun hashCodeTest() {
        val secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56))
        val id = mockUUID(leastSigBits = 0x12ab)
        val time = 123.milliseconds
        val encoded = byteArrayOf(0x76, 0x54, 0x32, 0x10)
        val it = mockTLSReceiver(
            secretKey = secretKey,
            id = id,
            time = time,
            encoded = encoded,
        )
        val expected = Objects.hash(
            secretKey.encoded.contentHashCode(),
            id,
            time,
            encoded.contentHashCode(),
        )
        val actual = it.hashCode()
        assertEquals(expected, actual)
    }

    @Test
    fun equalsTest() {
        val secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56))
        val id = mockUUID(leastSigBits = 0x12ab)
        val time = 123.milliseconds
        val encoded = byteArrayOf(0x76, 0x54, 0x32, 0x10)
        val it = mockTLSReceiver(
            secretKey = secretKey,
            id = id,
            time = time,
            encoded = encoded,
        )
        val it1 = mockTLSReceiver(
            secretKey = secretKey,
            id = id,
            time = time,
            encoded = encoded,
        )
        assertTrue(it == it1)
        listOf(
            mockTLSReceiver(
                secretKey = MockSecretKey(encoded = byteArrayOf(0x54, 0x32, 0x10)),
                id = id,
                time = time,
                encoded = encoded,
            ),
            mockTLSReceiver(
                secretKey = secretKey,
                id = mockUUID(leastSigBits = 0x34cd),
                time = time,
                encoded = encoded,
            ),
            mockTLSReceiver(
                secretKey = secretKey,
                id = id,
                time = 456.milliseconds,
                encoded = encoded,
            ),
            mockTLSReceiver(
                secretKey = secretKey,
                id = id,
                time = time,
                encoded = byteArrayOf(0x76, 0x54, 0x32, 0x10, 0x11),
            ),
        ).forEach { it2 ->
            assertFalse(it == it2)
        }
        assertNotEquals(it, Unit)
    }

    @Test
    fun buildTest() {
        val time = 12.milliseconds
        val secretKey = MockSecretKey(encoded = mockByteArray(13))
        val encryptedSK = mockByteArray(14)
        val encoded = mockByteArray(15)
        val id = mockUUID(16)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds) + toByteArray(id)
        val encrypted = mockByteArray(17)
        val signature = mockByteArray(18)
        val methodCode: Byte = 19
        val encodedQuery = mockByteArray(21)
        val signatureData = payload + methodCode + encodedQuery + secretKey.encoded
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(22)),
            publicKey = MockPublicKey(mockByteArray(23)),
        )
        val env = MockTLSEnvironment(
            timeProvider = { time },
            newSecretKeyProvider = { secretKey },
            items = listOf(
                Triple(keyPair.private.encoded, secretKey.encoded, encryptedSK),
                Triple(secretKey.encoded, payload, encrypted),
            ),
            keys = listOf(
                secretKey.encoded to secretKey,
            ),
            signs = listOf(
                keyPair to (signatureData to signature),
            ),
        )
        val body = toByteArray(encryptedSK.size) + encryptedSK +
            toByteArray(encrypted.size) + encrypted +
            toByteArray(signature.size) + signature
        val actual = TLSReceiver.build(
            env = env,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            body = body,
        )
        val expected = mockTLSReceiver(
            secretKey = secretKey,
            id = id,
            time = time,
            encoded = encoded,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun buildVerifiedErrorTest() {
        val time = 123.milliseconds
        val secretKey = MockSecretKey(encoded = mockByteArray(1))
        val encryptedSK = mockByteArray(11)
        val encoded = mockByteArray(4)
        val id = mockUUID(42)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds) + toByteArray(id)
        val encrypted = mockByteArray(12)
        val signature = mockByteArray(131)
        val signatureWrong = mockByteArray(121)
        check(!signature.contentEquals(signatureWrong))
        val methodCode: Byte = 23
        val encodedQuery = mockByteArray(3)
        val signatureData = payload + methodCode + encodedQuery + secretKey.encoded
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(21)),
            publicKey = MockPublicKey(mockByteArray(22)),
        )
        val env = MockTLSEnvironment(
            timeProvider = { time },
            newSecretKeyProvider = { secretKey },
            items = listOf(
                Triple(keyPair.private.encoded, secretKey.encoded, encryptedSK),
                Triple(secretKey.encoded, payload, encrypted),
            ),
            keys = listOf(
                secretKey.encoded to secretKey,
            ),
            signs = listOf(
                keyPair to (signatureData to signature),
            ),
        )
        val body = toByteArray(encryptedSK.size) + encryptedSK +
            toByteArray(encrypted.size) + encrypted +
            toByteArray(signatureWrong.size) + signatureWrong
        val expected = "Not verified!"
        val throwable: Throwable = assertThrows(IllegalStateException::class.java) {
            TLSReceiver.build(
                env = env,
                methodCode = methodCode,
                encodedQuery = encodedQuery,
                body = body,
            )
        }
        assertEquals(expected, throwable.message)
    }

    @Test
    fun toHttpResponseTest() {
        val time = 11.milliseconds
        val encoded = mockByteArray(12)
        val payload = toByteArray(encoded.size) + encoded + toByteArray(time.inWholeMilliseconds)
        val encrypted = mockByteArray(13)
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(14)),
        )
        val secretKey = MockSecretKey(mockByteArray(16))
        val methodCode: Byte = 0x1f
        val encodedQuery = "TLSReceiverTest:toHttpResponseTest:query".toByteArray()
        val requestID = mockUUID(17)
        val responseCode = 171
        val message = "TLSReceiverTest:toHttpResponseTest:message"
        val messageEncoded = message.toByteArray()
        val signatureData = payload + toByteArray(requestID) + methodCode + encodedQuery + toByteArray(responseCode) + messageEncoded
        val signature = mockByteArray(18)
        val env = MockTLSEnvironment(
            timeProvider = { time },
            items = listOf(
                Triple(secretKey.encoded, payload, encrypted),
            ),
            signs = listOf(
                keyPair to (signatureData to signature),
            ),
        )
        val tlsResponse = mockTLSResponse(
            code = responseCode,
            message = message,
            encoded = encoded,
        )
        val body = toByteArray(encrypted.size) + encrypted + toByteArray(signature.size) + signature
        val expected = mockHttpResponse(
            version = "1.1",
            code = responseCode,
            message = message,
            body = body,
        )
        val actual = TLSReceiver.toHttpResponse(
            env = env,
            secretKey = secretKey,
            methodCode = methodCode,
            encodedQuery = encodedQuery,
            requestID = requestID,
            tlsResponse = tlsResponse,
        )
        assertEquals(expected, actual)
    }
}
