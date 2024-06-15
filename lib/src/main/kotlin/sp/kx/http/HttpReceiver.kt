package sp.kx.http

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException

class HttpReceiver(
    private val routing: HttpRouting,
) {
    sealed interface State {
        data class Stopped(val starting: Boolean) : State
        data class Started(
            val stopping: Boolean,
            val host: String,
            val port: Int,
        ) : State
    }

    private val _states = MutableStateFlow<State>(State.Stopped(starting = false))
    val states = _states.asStateFlow()
    private var serverSocket: ServerSocket? = null

    private fun getLocalAddress(): Pair<NetworkInterface, InetAddress> {
        for (ni in NetworkInterface.getNetworkInterfaces()) {
            for (address in ni.inetAddresses) {
                if (address.isLoopbackAddress) continue
                if (address.isSiteLocalAddress) return ni to address
            }
        }
        TODO()
    }

    private fun onStarting(
        ni: NetworkInterface,
        serverSocket: ServerSocket,
    ) {
        while (true) {
            val state = _states.value
            if (state !is State.Started) break
            if (state.stopping) break
            try {
                println("socket accepting...") // todo
                serverSocket.accept().use { socket ->
                    println("socket(${socket.hashCode()}) accepted") // todo
                    val request = HttpRequest.read(socket.getInputStream())
                    println("socket(${socket.hashCode()}): request(${request.hashCode()}) read") // todo
                    val response = routing.route(request)
                    println("socket(${socket.hashCode()}): response(${response.hashCode()}) routed") // todo
                    response.write(socket.getOutputStream())
                }
            } catch (e: SocketException) {
                if (_states.value == state.copy(stopping = true)) break
                val isReachable = serverSocket.inetAddress.isReachable(ni, 0, 1_000)
                if (!isReachable) break
                TODO("socket accept error: $e")
            } catch (e: Throwable) {
                TODO("socket accept unknown error: $e")
            }
        }
    }

    fun start(port: Int? = null) {
        val state = _states.value
        if (state !is State.Stopped) TODO()
        if (state.starting) TODO()
        _states.value = State.Stopped(starting = true)
        runCatching {
            getLocalAddress()
        }.mapCatching { (ni, address) ->
            ni to ServerSocket(port ?: 0, 1, address)
        }.onSuccess { (ni, serverSocket) ->
            if (this.serverSocket != null) TODO()
            this.serverSocket = serverSocket
            _states.value = State.Started(
                stopping = false,
                host = serverSocket.inetAddress.hostAddress!!,
                port = serverSocket.localPort,
            )
            onStarting(ni = ni, serverSocket = serverSocket)
            this.serverSocket = null
        }
        _states.value = State.Stopped(starting = false)
    }

    fun stop() {
        val state = _states.value
        if (state !is State.Started) TODO()
        if (state.stopping) TODO()
        val serverSocket = serverSocket ?: TODO()
        _states.value = state.copy(stopping = true)
        runCatching { serverSocket.close() }
    }
}
