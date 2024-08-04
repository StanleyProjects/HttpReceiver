package sp.service.transmitter.provider

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import java.util.concurrent.TimeUnit

internal class FinalRemotes(
    private val address: URL,
) : Remotes {
    private val client = OkHttpClient.Builder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun double(number: Int): Int {
        val query = "/double"
        val method = "POST"
        val requestBody: ByteArray = ByteArray(0)
        return client.newCall(
            request = Request.Builder()
                .url(URL(address, query))
                .method(method, requestBody.toRequestBody())
                .build(),
        ).execute().use { response ->
            when (response.code) {
                200 -> {
                    TODO("FinalRemotes:double($number)")
                }
                else -> error("Unknown code: ${response.code}!")
            }
        }
    }
}
