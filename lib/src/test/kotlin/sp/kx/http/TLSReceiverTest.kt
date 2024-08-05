package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TLSReceiverTest {
    @Test
    fun toStringTest() {
        val expected = "{secretKey: \"123456\", id: 00000000-0000-0000-0000-0000000012ab, encoded: \"76543210\"}"
        val it = mockTLSReceiver(
            secretKey = MockSecretKey(encoded = byteArrayOf(0x12, 0x34, 0x56)),
            id = mockUUID(leastSigBits = 0x12ab),
            encoded = byteArrayOf(0x76, 0x54, 0x32, 0x10),
        )
        val actual = it.toString()
        assertEquals(expected, actual)
    }
}
