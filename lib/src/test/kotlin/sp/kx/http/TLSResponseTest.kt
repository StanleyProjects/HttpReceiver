package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Objects

internal class TLSResponseTest {
    @Test
    fun createTest() {
        val code = 123
        val message = "foo"
        val encoded = "foo:bar:encoded".toByteArray()
        val it = TLSResponse(
            code = code,
            message = message,
            encoded = encoded,
        )
        assertEquals(code, it.code)
        assertEquals(message, it.message)
        assertTrue(encoded.contentEquals(it.encoded))
    }

    @Test
    fun toStringTest() {
        val code = 123
        val message = "foo"
        val it = TLSResponse(
            code = code,
            message = message,
            encoded = byteArrayOf(0x12, 0x34, 0x56, 0x78),
        )
        val expected = "{code: $code, message: \"$message\", encoded: \"12345678\"}"
        val actual = it.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun equalsTest() {
        val code = 123
        val message = "foo"
        val encoded = "foo:bar:encoded".toByteArray()
        val it = TLSResponse(
            code = code,
            message = message,
            encoded = encoded,
        )
        val it1 = TLSResponse(
            code = code,
            message = message,
            encoded = encoded,
        )
        assertTrue(it == it1)
        listOf(
            TLSResponse(
                code = 1,
                message = message,
                encoded = encoded,
            ),
            TLSResponse(
                code = code,
                message = "bar",
                encoded = encoded,
            ),
            TLSResponse(
                code = code,
                message = message,
                encoded = "foo:bar:baz".toByteArray(),
            ),
        ).forEach { it2 ->
            assertFalse(it == it2)
        }
        assertNotEquals(it, Unit)
    }

    @Test
    fun hashCodeTest() {
        val code = 123
        val message = "foo"
        val encoded = "foo:bar:encoded".toByteArray()
        val it = TLSResponse(
            code = code,
            message = message,
            encoded = encoded,
        )
        val expected = Objects.hash(
            code,
            message,
            encoded.contentHashCode(),
        )
        val actual = it.hashCode()
        assertEquals(expected, actual)
    }
}
