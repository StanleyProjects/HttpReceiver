package sp.kx.http

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class TLSRoutingTest {
    @Test
    fun onReceiverTest() {
        val time = 12.milliseconds
        val timeNow = time + 1.seconds
        check(timeNow >= time)
        val timeMax = 1.minutes
        check(timeNow - time <= timeMax)
        val newID = mockUUID(0x1234)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            timeProvider = { timeNow },
            newUUIDProvider = { newID },
        )
        val oldID = mockUUID(0xabcd)
        check(newID != oldID)
        val routing = MockTLSRouting(
            env = env,
            requested = mapOf(
                oldID to (timeNow - timeMax - 1.minutes),
            ),
        )
        val receiver = mockTLSReceiver(
            id = newID,
            time = time,
        )
        routing.requested().entries.single().also { (id, t) ->
            check(id == oldID)
            check(t == (timeNow - timeMax - 1.minutes))
            check(timeNow - t > timeMax)
        }
        routing.onReceiver(receiver)
        routing.requested().entries.single().also { (id, t) ->
            assertEquals(newID, id)
            assertEquals(time, t)
        }
    }

    @Test
    fun onReceiverRequestedTest() {
        val time = 12.milliseconds
        val timeNow = time + 1.seconds
        check(timeNow >= time)
        val timeMax = 1.minutes
        check(timeNow - time <= timeMax)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            timeProvider = { timeNow },
            newUUIDProvider = { mockUUID(21) },
        )
        val routing = MockTLSRouting(
            env = env,
            requested = mapOf(
                mockUUID(22) to (time + 2.seconds),
                mockUUID(23) to (timeNow - timeMax - 1.minutes),
            ),
        )
        val receiver = mockTLSReceiver(
            id = mockUUID(21),
            time = time,
        )
        check(routing.requested().size == 2)
        routing.requested().keys.forEach { id ->
            check(id != mockUUID(21))
        }
        routing.onReceiver(receiver)
        val expected = mapOf(
            mockUUID(21) to time,
            mockUUID(22) to (time + 2.seconds),
        ).entries.toList().sortedBy { (id, _) -> id }
        assertEquals(expected, routing.requested().entries.toList().sortedBy { (id, _) -> id })
    }

    @Test
    fun onReceiverRequestedEmptyTest() {
        val time = 12.milliseconds
        val timeNow = time + 1.seconds
        check(timeNow >= time)
        val timeMax = 1.minutes
        check(timeNow - time <= timeMax)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            timeProvider = { timeNow },
            newUUIDProvider = { mockUUID(21) },
        )
        val routing = MockTLSRouting(env = env)
        val receiver = mockTLSReceiver(
            id = mockUUID(21),
            time = time,
        )
        check(routing.requested().isEmpty())
        routing.onReceiver(receiver)
        val expected = mapOf(
            mockUUID(21) to time,
        ).entries.toList().sortedBy { (id, _) -> id }
        assertEquals(expected, routing.requested().entries.toList().sortedBy { (id, _) -> id })
    }

    @Test
    fun onReceiverTimeErrorTest() {
        val time = 12.milliseconds
        val timeNow = time - 1.seconds
        check(timeNow < time)
        val env = MockTLSEnvironment(
            timeProvider = { timeNow },
        )
        val routing = MockTLSRouting(env = env)
        val receiver = mockTLSReceiver(time = time)
        val throwable: IllegalStateException = assertThrows(IllegalStateException::class.java) {
            routing.onReceiver(receiver)
        }
        assertEquals("Time error!", throwable.message)
    }

    @Test
    fun onReceiverTimeIsUpTest() {
        val time = 12.milliseconds
        val timeMax = 1.minutes
        val timeNow = time + 1.seconds + timeMax
        check(timeNow >= time)
        check(timeNow - time >= timeMax)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            timeProvider = { timeNow },
        )
        val routing = MockTLSRouting(env = env)
        val receiver = mockTLSReceiver(time = time)
        val throwable: IllegalStateException = assertThrows(IllegalStateException::class.java) {
            routing.onReceiver(receiver)
        }
        assertEquals("Time is up!", throwable.message)
    }

    @Test
    fun onReceiverIDErrorTest() {
        val time = 12.milliseconds
        val timeNow = time + 1.seconds
        check(timeNow >= time)
        val timeMax = 1.minutes
        check(timeNow - time <= timeMax)
        val id = mockUUID(13)
        val env = MockTLSEnvironment(
            timeMax = timeMax,
            timeProvider = { timeNow },
        )
        val routing = MockTLSRouting(
            env = env,
            requested = mapOf(id to Duration.ZERO)
        )
        val receiver = mockTLSReceiver(
            id = id,
            time = time,
        )
        val throwable: IllegalStateException = assertThrows(IllegalStateException::class.java) {
            routing.onReceiver(receiver)
        }
        assertEquals("Request ID error!", throwable.message)
    }

    @Test
    fun mapTest() {
        val time = 12.milliseconds
        val secretKey = MockSecretKey(encoded = mockByteArray(13))
        val encryptedSK = mockByteArray(14)
        val responseDecoded: Int = 654321
        val requestDecoded: String = responseDecoded.toString()
        val requestEncoded = requestDecoded.toByteArray()
        val id = mockUUID(0xdcba)
        val requestPayload = toByteArray(requestEncoded.size) + requestEncoded + toByteArray(time.inWholeMilliseconds) + toByteArray(id)
        val requestEncrypted = mockByteArray(17)
        val requestSignature = mockByteArray(18)
        val methodCode: Byte = 1
        val query = "foobarbaz"
        val encodedQuery = query.toByteArray()
        val requestSignatureData = requestPayload + methodCode + encodedQuery + secretKey.encoded
        val keyPair = mockKeyPair(
            privateKey = MockPrivateKey(mockByteArray(22)),
            publicKey = MockPublicKey(mockByteArray(23)),
        )
        val responseEncoded = mockByteArray(12)
        val responsePayload = toByteArray(responseEncoded.size) + responseEncoded + toByteArray(time.inWholeMilliseconds)
        val responseEncrypted = mockByteArray(13)
        val responseSignatureData = responsePayload + toByteArray(id) + methodCode + encodedQuery
        val responseSignature = mockByteArray(18)
        val env = MockTLSEnvironment(
            timeProvider = { time },
            newSecretKeyProvider = { secretKey },
            items = listOf(
                Triple(keyPair.private.encoded, secretKey.encoded, encryptedSK),
                Triple(secretKey.encoded, requestPayload, requestEncrypted),
                Triple(secretKey.encoded, responsePayload, responseEncrypted),
            ),
            keys = listOf(
                secretKey.encoded to secretKey,
            ),
            signs = listOf(
                keyPair to (requestSignatureData to requestSignature),
                keyPair to (responseSignatureData to responseSignature),
            ),
        )
        val requestBody = toByteArray(encryptedSK.size) + encryptedSK +
            toByteArray(requestEncrypted.size) + requestEncrypted +
            toByteArray(requestSignature.size) + requestSignature
        val routing = MockTLSRouting(
            keyPair = keyPair,
            env = env,
        )
        val responseBody = toByteArray(responseEncrypted.size) + responseEncrypted + toByteArray(responseSignature.size) + responseSignature
        val expected = mockHttpResponse(
            version = "1.1",
            code = 200,
            message = "OK",
            headers = emptyMap(),
            body = responseBody,
        )
        val actual = routing.test(
            request = mockHttpRequest(
                method = "POST",
                query = query,
                body = requestBody,
            ),
            transform = { bytes ->
                check(bytes.contentEquals(requestEncoded))
                mockTLSResponse(
                    encoded = responseEncoded,
                )
            },
        )
        assertEquals(expected, actual)
    }
}
