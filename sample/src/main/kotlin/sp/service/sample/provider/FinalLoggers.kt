package sp.service.sample.provider

internal class FinalLoggers : Loggers {
    override fun create(tag: String): Logger {
        return SystemLogger(tag = tag)
    }
}

private class SystemLogger(private val tag: String) : Logger {
    override fun debug(message: String) {
        println("$tag: $message\n")
    }
}
