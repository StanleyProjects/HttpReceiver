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
        val builder = StringBuilder("{")
        if (version.isEmpty()) {
            builder.append("code: $code")
        } else {
            builder
                .append("version: \"$version\"")
                .append(", ")
                .append("code: $code")
        }
        if (message.isNotEmpty()) {
            builder
                .append(", ")
                .append("message: \"$message\"")
        }
        if (headers.isNotEmpty()) {
            builder
                .append(", ")
                .append("headers: $headers")
        }
        if (body != null && body.isNotEmpty()) {
            builder
                .append(", ")
                .append("body: \"${body.toHEX()}\"")
        }
        return builder.append("}").toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is HttpResponse -> {
                val equals = when {
                    body == null -> other.body == null
                    other.body == null -> false
                    else -> body.contentEquals(other.body)
                }
                return equals && other.version == version && other.code == code && other.message == message && other.headers == headers
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

        fun NotFound(
            headers: Map<String, String> = emptyMap(),
            body: ByteArray? = null,
        ): HttpResponse {
            return HttpResponse(
                version = "1.1",
                code = 404,
                message = "Not Found",
                headers = headers,
                body = body,
            )
        }

        fun MethodNotAllowed(
            headers: Map<String, String> = emptyMap(),
            body: ByteArray? = null,
        ): HttpResponse {
            return HttpResponse(
                version = "1.1",
                code = 405,
                message = "Method Not Allowed",
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
