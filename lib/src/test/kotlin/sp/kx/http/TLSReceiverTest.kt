package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TLSReceiverTest {
    @Test
    fun toStringTest() {
        val expected = ""
        val it = mockTLSReceiver()
        val actual = it.toString()
        assertEquals(expected, actual)
    }
}
