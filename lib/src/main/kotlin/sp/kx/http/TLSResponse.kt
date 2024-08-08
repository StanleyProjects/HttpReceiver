package sp.kx.http

import sp.kx.bytes.toHEX
import java.util.Objects

class TLSResponse(
    val code: Int,
    val message: String,
    val encoded: ByteArray?,
) {
    override fun toString(): String {
        val builder = StringBuilder("{")
            .append("code: $code")
        if (message.isNotEmpty()) {
            builder.append(", message: \"$message\"")
        }
        if (encoded != null && encoded.isNotEmpty()) {
            builder.append(", encoded: \"${encoded.toHEX()}\"")
        }
        return builder.append("}").toString()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is TLSResponse -> {
                val equals = when {
                    encoded == null -> other.encoded == null
                    other.encoded == null -> false
                    else -> encoded.contentEquals(other.encoded)
                }
                equals && code == other.code && message == other.message
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            code,
            message,
            encoded?.contentHashCode() ?: 0,
        )
    }
}
