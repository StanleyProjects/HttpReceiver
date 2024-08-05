package sp.kx.http

import sp.kx.bytes.write
import java.util.UUID

internal fun mockByteArray(size: Int): ByteArray {
    val bytes = ByteArray(size)
    for (i in 0 until size) {
        bytes[i] = (size - i).toByte()
    }
    return bytes
}

internal fun toByteArray(value: Int): ByteArray {
    val bytes = ByteArray(4)
    bytes.write(value = value)
    return bytes
}

internal fun toByteArray(value: Long): ByteArray {
    val bytes = ByteArray(8)
    bytes.write(value = value)
    return bytes
}

internal fun toByteArray(value: UUID): ByteArray {
    val bytes = ByteArray(16)
    bytes.write(value = value)
    return bytes
}
