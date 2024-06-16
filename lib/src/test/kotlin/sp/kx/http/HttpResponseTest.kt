package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class HttpResponseTest {
    @Test
    fun createTest() {
        val version = "42"
        val code = 123
        val message = "foo"
        val headers = mapOf("foo" to "bar")
        val body = "bar".toByteArray()
        val response = HttpResponse(
            version = version,
            code = code,
            message = message,
            headers = headers,
            body = body,
        )
        assertEquals(version, response.version)
        assertEquals(code, response.code)
        assertEquals(message, response.message)
        assertEquals(headers.toList().sortedBy { it.first }, response.headers.toList().sortedBy { it.first })
        assertTrue(body.contentEquals(response.body))
    }

    @Test
    fun writeTest() {
        val version = "42"
        val code = 123
        val message = "foo"
        val body = "bar".toByteArray()
        val headers = mapOf("Content-Length" to body.size.toString())
        val response = HttpResponse(
            version = version,
            code = code,
            message = message,
            headers = headers,
            body = body,
        )
        val stream = ByteArrayOutputStream()
        response.write(stream)
        val actual = String(stream.toByteArray())
        val expected = """
            HTTP/$version $code $message
            Content-Length: ${body.size}
            
            ${String(body)}
        """.trimIndent().replace("\n", "\r\n")
        assertEquals(expected, actual)
    }
}
