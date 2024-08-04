package sp.service.transmitter.provider

internal interface Secrets {
    fun hash(bytes: ByteArray): ByteArray
}
