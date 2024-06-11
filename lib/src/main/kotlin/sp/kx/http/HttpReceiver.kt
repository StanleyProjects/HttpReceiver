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

    private val _state = MutableStateFlow<State>(State.Stopped(starting = false))
    val state = _state.asStateFlow()
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
            val currentState = _state.value
            if (currentState !is State.Started) break
            if (currentState.stopping) break
            try {
                serverSocket.accept().use { socket ->
                    val request = HttpRequest.read(socket.getInputStream())
                    val response = routing.route(request)
                    response.write(socket.getOutputStream())
                }
            } catch (e: SocketException) {
                if (_state.value == currentState.copy(stopping = true)) break
                val isReachable = serverSocket.inetAddress.isReachable(ni, 0, 1_000)
                if (!isReachable) break
                TODO("socket accept error: $e")
            } catch (e: Throwable) {
                TODO("socket accept unknown error: $e")
            }
        }
    }

    fun start() {
        val oldState = _state.value
        if (oldState !is State.Stopped) TODO()
        if (oldState.starting) TODO()
        _state.value = State.Stopped(starting = true)
        runCatching {
            getLocalAddress()
        }.mapCatching { (ni, address) ->
            val port = 40631
            ni to ServerSocket(port, 1, address)
        }.onSuccess { (ni, serverSocket) ->
            if (this.serverSocket != null) TODO()
            this.serverSocket = serverSocket
            _state.value = State.Started(
                stopping = false,
                host = serverSocket.inetAddress.hostAddress!!,
                port = serverSocket.localPort,
            )
            onStarting(ni = ni, serverSocket = serverSocket)
            this.serverSocket = null
        }
        _state.value = State.Stopped(starting = false)
    }

    fun stop() {
        val oldState = _state.value
        if (oldState !is State.Started) TODO()
        if (oldState.stopping) TODO()
        val serverSocket = serverSocket ?: TODO()
        _state.value = oldState.copy(stopping = true)
        runCatching { serverSocket.close() }
    }
}
