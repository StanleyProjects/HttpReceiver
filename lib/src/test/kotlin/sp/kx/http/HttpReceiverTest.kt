package sp.kx.http

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class HttpReceiverTest {
    @Test
    fun createTest() {
        val routing = object : HttpRouting {
            override fun route(request: HttpRequest): HttpResponse {
                error("Impossible!")
            }
        }
        val receiver = HttpReceiver(routing)
        val expected = HttpReceiver.State.Stopped(starting = false)
        Assertions.assertEquals(expected, receiver.states.value)
    }
}
