package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Objects

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
}
