package sp.kx.http

import java.io.OutputStream

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
