package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TLSRoutingTest {
    @Test
    fun mapTest() {
        val keyPair = mockKeyPair()
        val routing = MockTLSRouting()
        val httpRequest = mockHttpRequest(
            body = ByteArray(8),
        )
        val expected = mockHttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
        )
        // todo
    }
}
