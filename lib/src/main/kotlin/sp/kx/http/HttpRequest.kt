package sp.kx.http

import sp.kx.http.HttpRequest.Companion.toByteArray
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
        private fun ByteArray.readLine(index: Int, separator: ByteArray): String {
            for (x in index until size) {
                if (get(x) != separator[0]) continue
                val slice = copyOfRange(x, x + separator.size)
                println("slice($x..${x + separator.lastIndex}): ${slice.map { String.format("%02x(%+04d)", it.toInt() and 0xff, it) }}")
                if (separator.contentEquals(slice)) {
                    return String(copyOfRange(index, x))
                }
            }
            return String(copyOfRange(index, size))
        }

        private fun InputStream.foo(): String {
            val buffer = ByteArrayOutputStream()
            while (available() > 0) {
                val bytes = ByteArray(512)
                val length = read(bytes)
                if (length < 0) break
                buffer.write(bytes, 0, length)
            }
            val bytes = buffer.toByteArray()
            println("bytes: ${bytes.size}")
            val separator = "\r\n".toByteArray()
            println("separator: ${separator.map { String.format("%02x(%+04d)", it.toInt() and 0xff, it) }}")
            var index = 0
            val builder = StringBuilder()
            val firstLine = bytes.readLine(index = index, separator = separator)
            check(firstLine.isNotEmpty())
            index += firstLine.length + separator.size
            builder.append(firstLine)
            while (true) {
                val line = bytes.readLine(index = index, separator = separator)
                if (line.isEmpty()) break
                index += line.length + separator.size
                builder.append("\n").append(line)
            }
            return builder.toString()
        }

        private fun InputStream.forEach(block: (Int, Int, Byte) -> Unit): Int {
            var size = 0
            while (available() > 0) {
                val separator = "\r\n".toByteArray()
                val bytes = ByteArray(512)
                val length = read(bytes)
                println("read bytes($length): available: ${available()}") // todo
                if (length < 0) break
                for (i in 0 until length) {
                    block(length, i, bytes[i])
                }
                size += length
            }
            return size
        }

        private fun InputStream.toByteArray(): ByteArray {
            val buffer = ByteArrayOutputStream()
            while (available() > 0) {
                val bytes = ByteArray(1024)
                val length = read(bytes)
                println("read bytes($length): available: ${available()}") // todo
                if (length < 0) break
                buffer.write(bytes, 0, length)
            }
            return buffer.toByteArray()
        }

        internal fun read(inputStream: InputStream): HttpRequest {
            val bytes = inputStream.toByteArray()
            val separator = "\r\n".toByteArray()
            var index = 0
            val firstLine = bytes.readLine(index = index, separator = separator)
            index += firstLine.length + separator.size
            val split = firstLine.split(" ")
            check(split.size == 3) { "Wrong first header!" }
            val protocol = split[2].split("/")
            check(protocol.size == 2) { "Wrong protocol!" }
            check(protocol[0] == "HTTP")
            val version = protocol[1]
            check(version == "1.1") // todo 505 HTTP Version Not Supported
            val method = split[0]
            println("read method: $method") // todo
            val query = split[1]
            println("read query: $query") // todo
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = bytes.readLine(index = index, separator = separator)
                if (line.isEmpty()) break
                index += line.length + separator.size
                println("read line: ${line.hashCode()}") // todo
                val colonIndex = line.indexOf(':')
                if (colonIndex < 1) continue
                if (colonIndex > line.length - 3) continue
                val key = line.substring(0, colonIndex)
                val value = line.substring(colonIndex + 2, line.length)
                headers[key] = value
                println("read header: \"$key\": \"$value\"") // todo
            }
            index += separator.size
//            for (i in index until bytes.size) {
//                val it = bytes[i]
//                val message = String.format("read byte:%03d/${bytes.size}: %02x(%+04d)", i + 1, it.toInt() and 0xff, it)
//                println(message) // todo
//            }
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

        internal fun readV4(inputStream: InputStream): HttpRequest {
//            val size = inputStream.forEach { length, index, it ->
//                val message = String.format("read byte:%03d/%03d: %02x(%+04d)", index + 1, length, it.toInt() and 0xff, it)
//                println(message) // todo
//            }
//            println("read bytes: $size") // todo
            val text = inputStream.foo()
            println("read bytes:\n---\n$text\n---\n") // todo
            return HttpRequest(
                version = "1.1",
                method = "todo",
                query = "todo",
                headers = emptyMap(),
                body = null,
            )
        }

        internal fun readV3(inputStream: InputStream): HttpRequest {
            val bytes = inputStream.toByteArray()
            println("read bytes(${bytes.size}): ${bytes.contentHashCode()}") // todo
            return HttpRequest(
                version = "1.1",
                method = "todo",
                query = "todo",
                headers = emptyMap(),
                body = null,
            )
        }

        internal fun readV2(inputStream: InputStream): HttpRequest {
            val reader = inputStream.bufferedReader()
            val firstHeader = reader.readLine()
            println("read header(${firstHeader.length}): ${firstHeader.hashCode()}:\n---\n${firstHeader}\n---") // todo
            var contentLength: Int? = null
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length: ")) {
                    line.substring("Content-Length: ".length).toIntOrNull()?.let {
                        contentLength = it
                    }
                }
                println("read line(${line.length}): ${line.hashCode()}:\n---\n${line}\n---") // todo
            }
            val length = contentLength
            if (length != null && length > 0) {
                var index = 0
                while (true) {
                    val it = reader.read()
                    if (it < 0) {
                        println("read is negative") // todo
                        break
                    }
                    println("read byte(${index + 1}/$length): $it") // todo
                    index++
                    if (index == length) {
                        println("read final") // todo
                        break
                    }
                }
            }
            return HttpRequest(
                version = "1.1",
                method = "todo",
                query = "todo",
                headers = emptyMap(),
                body = null,
            )
        }

        internal fun readV1(inputStream: InputStream): HttpRequest {
            val reader = inputStream.toByteArray().inputStream().bufferedReader()
            val firstHeader = reader.readLine()
            check(firstHeader != null) { "No first header!" }
            check(firstHeader.isNotEmpty()) { "First header is empty!" }
            check(firstHeader.isNotBlank()) { "First header is blank!" }
            val split = firstHeader.split(" ")
            check(split.size == 3) { "Wrong first header!" }
            val protocol = split[2].split("/")
            check(protocol.size == 2) { "Wrong protocol!" }
            check(protocol[0] == "HTTP")
            val version = protocol[1]
            check(version == "1.1") // todo 505 HTTP Version Not Supported
            val method = split[0]
            println("read method: $method") // todo
            val query = split[1]
            println("read query: $query") // todo
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = reader.readLine()
                println("read line: ${line.hashCode()}") // todo
                if (line.isNullOrEmpty()) break
                val index = line.indexOf(':')
                if (index < 1) continue
                if (index > line.length - 3) continue
                val key = line.substring(0, index)
                val value = line.substring(index + 2, line.length)
                headers[key] = value
                println("read header: \"$key\": \"$value\"") // todo
            }
            val body = headers["Content-Length"]
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let { size ->
                    println("read body: $size") // todo
//                    inputStream.toByteArray()
                    //
//                    val builder = StringBuilder()
//                    while (true) {
//                        val line = reader.readLine()
//                        if (line == null) break
//                        println("read line(${line.length}): ${line.hashCode()}:\n---\n$line\n---") // todo
//                        builder.append(builder)
//                    }
//                    builder.toString().toByteArray()
                    //
                    val bytes = ByteArray(size)
                    for (i in 0 until size) {
                        val it = reader.read().toByte()
                        val message = String.format("read byte:%03d/$size: %02x(%+04d)", i + 1, it.toInt() and 0xff, it)
                        println(message) // todo
                        bytes[i] = it
                    }
                    println("read all $size bytes") // todo
                    bytes
                    //
//                    ByteArray(size) {
//                        reader.read().toByte()
//                    }
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
