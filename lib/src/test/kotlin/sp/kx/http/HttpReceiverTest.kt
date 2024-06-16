package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HttpReceiverTest {
    @Test
    fun createTest() {
        val receiver = HttpReceiver(MockRouting())
        val expected = HttpReceiver.State.Stopped(starting = false)
        assertEquals(expected, receiver.states.value)
    }
}
