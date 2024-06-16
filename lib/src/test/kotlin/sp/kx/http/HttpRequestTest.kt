package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream

internal class HttpRequestTest {
    @Test
    fun createTest() {
        val version = "42"
        val method = "GET"
        val query = "/foo"
        val headers = mapOf("foo" to "bar")
        val body = "foobar".toByteArray()
        val request = HttpRequest(
            version = version,
            method = method,
            query = query,
            headers = headers,
            body = body,
        )
        assertEquals(version, request.version)
        assertEquals(method, request.method)
        assertEquals(query, request.query)
        assertEquals(headers.toList().sortedBy { it.first }, request.headers.toList().sortedBy { it.first })
        assertTrue(body.contentEquals(request.body))
    }

    @Test
    fun readTest() {
        val version = "1.1"
        val method = "GET"
        val query = "/foo"
        val body = "foobar".toByteArray()
        val headers = mapOf("Content-Length" to body.size.toString())
        val stream: InputStream = """
            $method $query HTTP/$version
            Content-Length: ${body.size}
            
            ${String(body)}
        """.trimIndent().replace("\n", "\r\n").byteInputStream()
        val request = HttpRequest.read(stream)
        assertEquals(version, request.version)
        assertEquals(method, request.method)
        assertEquals(query, request.query)
        assertEquals(headers.toList().sortedBy { it.first }, request.headers.toList().sortedBy { it.first })
        assertTrue(body.contentEquals(request.body))
    }
}
