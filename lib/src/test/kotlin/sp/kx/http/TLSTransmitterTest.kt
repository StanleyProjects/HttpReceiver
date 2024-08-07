package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Objects
import kotlin.time.Duration.Companion.milliseconds

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
            keyPair = keyPair,
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
}
