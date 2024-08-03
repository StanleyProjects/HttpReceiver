package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TLSRoutingTest {
    @Test
    fun mapTest() {
        val keyPair = mockKeyPair()
        val routing = MockTLSRouting(
            keyPairProvider = { keyPair },
        )
        val httpRequest = mockHttpRequest(
            body = ByteArray(8),
        )
        val expected = mockHttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
        )
        val actual = routing.map(
            request = httpRequest,
            decode = {
                TODO("TLSRoutingTest:mapTest:decode")
            },
            transform = {
                TODO("TLSRoutingTest:mapTest:transform")
            },
            encode = {
                TODO("TLSRoutingTest:mapTest:encode")
            },
        )
        assertEquals(expected, actual)
    }
}
