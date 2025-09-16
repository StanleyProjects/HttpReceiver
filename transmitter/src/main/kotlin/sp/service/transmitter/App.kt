package sp.service.transmitter

import sp.kx.bytes.hex
import sp.kx.secrets.Asymmetric
import sp.kx.secrets.Symmetric
import sp.kx.tlsmessages.RealTLSTransmitter
import sp.service.transmitter.provider.FinalLoggers
import sp.service.transmitter.provider.FinalRemotes
import sp.service.transmitter.provider.Loggers
import sp.service.transmitter.provider.Remotes
import java.net.URL
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import kotlin.time.Duration.Companion.minutes

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    val keyStore = KeyStore.getInstance("PKCS12")
    val alias = "a202"
    val password = "qwe202"
    Thread.currentThread().contextClassLoader.getResourceAsStream("a202.pkcs12").use {
        if (it == null) error("No stream!")
        logger.debug("load key store...")
        keyStore.load(it, password.toCharArray())
    }
    val key = keyStore.getKey(alias, password.toCharArray()) ?: error("No \"$alias\"!")
    check(key is PrivateKey)
    val certificate = keyStore.getCertificate(alias)
    val keyPair = KeyPair(certificate.publicKey, key)
    val remotes: Remotes = FinalRemotes(
        loggers = loggers,
        transmitter = RealTLSTransmitter(
            keyPair = keyPair,
            symmetric = Symmetric.AES,
            asymmetric = Asymmetric.RSA,
        ),
        address = URL("http://192.168.88.228:40631"),
    )
    val number = 42
    val expected = number * 2
    val actual = remotes.double(number = number)
    if (expected != actual) {
        error("expected: $expected, actual: $actual")
    }
}
