package sp.kx.http

internal class MockRouting : HttpRouting {
    override fun route(request: HttpRequest): HttpResponse {
        error("mock")
    }
}
