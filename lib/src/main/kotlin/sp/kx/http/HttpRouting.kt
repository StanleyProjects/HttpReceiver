package sp.kx.http

interface HttpRouting {
    fun route(request: HttpRequest): HttpResponse
}
