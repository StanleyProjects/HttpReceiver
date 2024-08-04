package sp.service.transmitter.provider

internal interface Loggers {
    fun create(tag: String): Logger
}

internal interface Logger {
    fun debug(message: String)
}
