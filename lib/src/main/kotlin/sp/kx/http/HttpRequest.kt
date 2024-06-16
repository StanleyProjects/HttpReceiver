package sp.kx.http

import java.io.ByteArrayOutputStream
import java.io.InputStream

class HttpRequest(
    val version: String,
    val method: String,
    val query: String,
    val headers: Map<String, String>,
    val body: ByteArray?,
) {
    companion object {
        private fun ByteArray.readUntil(index: Int, expected: ByteArray): String {
            if (expected.isEmpty()) TODO("Expected bytes is empty!")
            for (x in index until size) {
                if (get(x) != expected[0]) continue
                val toIndexExclusive = x + expected.size
                if (toIndexExclusive > size) break
                val slice = copyOfRange(x, toIndex = toIndexExclusive)
                if (!expected.contentEquals(slice)) TODO("Slice is not a separator!")
                return String(copyOfRange(index, x))
            }
            return String(copyOfRange(index, size))
        }

        private fun InputStream.toByteArray(): ByteArray {
            val buffer = ByteArrayOutputStream()
            while (true) {
                val bytes = ByteArray(1024)
                val length = read(bytes)
                if (length == 0) TODO("Stream returns 0 on read!")
                if (length < 0) break
                buffer.write(bytes, 0, length)
                val available = available()
                if (available == 0) break
            }
            return buffer.toByteArray()
        }

        internal fun read(inputStream: InputStream): HttpRequest {
            val bytes = inputStream.toByteArray()
            val separator = "\r\n".toByteArray()
            var index = 0
            val firstLine = bytes.readUntil(index = index, expected = separator)
            check(firstLine.isNotEmpty()) { "First header is empty!" }
            check(firstLine.isNotBlank()) { "First header is blank!" }
            index += firstLine.length + separator.size
            val split = firstLine.split(" ")
            check(split.size == 3) { "Wrong first header!" }
            val protocol = split[2].split("/")
            check(protocol.size == 2) { "Wrong protocol!" }
            check(protocol[0] == "HTTP")
            val version = protocol[1]
            check(version == "1.1") // todo 505 HTTP Version Not Supported
            val method = split[0]
            val query = split[1]
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = bytes.readUntil(index = index, expected = separator)
                if (line.isEmpty()) break
                index += line.length + separator.size
                val colonIndex = line.indexOf(':')
                if (colonIndex < 1) continue
                if (colonIndex > line.length - 3) continue
                val key = line.substring(0, colonIndex)
                val value = line.substring(colonIndex + 2, line.length)
                headers[key] = value
            }
            index += separator.size
            val body = headers["Content-Length"]
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let { size ->
                    bytes.copyOfRange(index, index + size)
                }
            return HttpRequest(
                version = version,
                method = method,
                query = query,
                headers = headers,
                body = body,
            )
        }
    }
}
