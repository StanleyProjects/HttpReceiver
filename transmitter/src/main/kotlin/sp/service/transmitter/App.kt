package sp.service.transmitter

import sp.kx.bytes.toHEX
import sp.service.transmitter.provider.FinalLoggers
import sp.service.transmitter.provider.FinalRemotes
import sp.service.transmitter.provider.FinalSecrets
import sp.service.transmitter.provider.Loggers
import sp.service.transmitter.provider.Remotes
import sp.service.transmitter.provider.Secrets
import java.net.URL
import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    val secrets: Secrets = FinalSecrets()
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
    logger.debug("private:key:hash: ${secrets.hash(key.encoded).toHEX()}")
    val certificate = keyStore.getCertificate(alias)
    logger.debug("public:key:hash: ${secrets.hash(certificate.publicKey.encoded).toHEX()}")
    val keyPair = KeyPair(certificate.publicKey, key)
    val remotes: Remotes = FinalRemotes(address = URL("http://192.168.88.222:40631"))
    val number = 42
    val expected = number * 2
    val actual = remotes.double(number = 42)
    if (expected != actual) {
        error("expected: $expected, actual: $actual")
    }
}
