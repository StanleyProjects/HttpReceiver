package sp.service.transmitter

import sp.service.transmitter.provider.FinalLoggers
import sp.service.transmitter.provider.Loggers

fun main() {
    val loggers: Loggers = FinalLoggers()
    val logger = loggers.create("[App]")
    TODO("${Thread.currentThread().contextClassLoader.name}:main")
}
