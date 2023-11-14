package br.com.windel.pos.gateways

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class MyWebSocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("open")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println("message")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("falha")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        println("close")
    }
}

fun connectWebSocket(serialNumber: String, WINDEL_POS_API_KEY: String, WINDEL_POS_HOST: String): WebSocket {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            // Não implementado para confiar em todos os clientes
        }

        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
            // Não implementado para confiar em todos os servidores
        }

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    })

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, null)

    val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .build()

    val request = Request.Builder()
        .url(WINDEL_POS_HOST)
        .build()

    val webSocketListener = MyWebSocketListener()

    return okHttpClient.newWebSocket(request, webSocketListener)
}
