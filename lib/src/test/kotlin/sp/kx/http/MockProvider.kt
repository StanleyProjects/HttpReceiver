package sp.kx.http

internal fun interface MockProvider<T : Any> {
    fun provide(): T
}
