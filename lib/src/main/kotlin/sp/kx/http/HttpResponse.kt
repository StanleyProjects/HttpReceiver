package sp.kx.http

import sp.kx.bytes.toHEX
import java.io.OutputStream
import java.util.Objects

/**
 * A class to represent a message sent by the server in response to a message from the client.
 *
 * @property code A status [code](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status),
 * indicating success or failure of the request.
 * @property message A status text. A brief, purely informational,
 * textual description of the status code to help a human understand the HTTP message.
 * @author [Stanley Wintergreen](https://github.com/kepocnhh)
 * @since 0.0.2
 */
class HttpResponse(
    val version: String,
    val code: Int,
    val message: String,
    val headers: Map<String, String>,
    val body: ByteArray?,
) {
    internal fun write(outputStream: OutputStream) {
        val builder = StringBuilder()
            .append("HTTP/$version $code $message")
            .append("\r\n")
        headers.forEach { (key, value) ->
            builder.append("$key: $value")
                .append("\r\n")
        }
        val bytes = builder
            .append("\r\n")
            .toString()
            .toByteArray()
        outputStream.write(bytes)
        if (body != null) {
            outputStream.write(body)
        }
        outputStream.flush()
    }

    override fun toString(): String {
        return "{" +
            "version: $version, " +
            "code: $code, " +
            "message: $message, " +
            "headers: $headers, " +
            "body: ${body?.toHEX()}" +
            "}"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is HttpResponse -> {
                val equals = body == null && other.body == null || body != null && other.body != null && body.contentEquals(other.body)
                if (!equals) return false
                return other.version == version && other.code == code && other.message == message && other.headers == headers
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            version,
            code,
            message,
            headers.entries.sumOf { (k, v) -> Objects.hash(k, v) },
            body?.contentHashCode() ?: 0,
        )
    }

    companion object {
        fun OK(
            headers: Map<String, String> = emptyMap(),
            body: ByteArray? = null,
        ): HttpResponse {
            return HttpResponse(
                version = "1.1",
                code = 200,
                message = "OK",
                headers = headers,
                body = body,
            )
        }

        fun InternalServerError(
            headers: Map<String, String> = emptyMap(),
            body: ByteArray? = null,
        ): HttpResponse {
            return HttpResponse(
                version = "1.1",
                code = 500,
                message = "Internal Server Error",
                headers = headers,
                body = body,
            )
        }
    }
}
