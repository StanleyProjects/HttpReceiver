package sp.service.transmitter.provider

import java.security.MessageDigest

internal class FinalSecrets : Secrets {
    override fun hash(bytes: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA256")
        return md.digest(bytes)
    }
}
