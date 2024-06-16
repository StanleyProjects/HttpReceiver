package sp.kx.http

import java.io.OutputStream

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
}
