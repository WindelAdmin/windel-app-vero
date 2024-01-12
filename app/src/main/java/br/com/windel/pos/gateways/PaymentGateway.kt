
import android.os.Build.SERIAL
import android.util.Base64
import br.com.windel.pos.BuildConfig
import br.com.windel.pos.enums.EventsEnum.EVENT_CANCELED
import br.com.windel.pos.enums.EventsEnum.EVENT_FAILED
import br.com.windel.pos.enums.EventsEnum.EVENT_PAY
import br.com.windel.pos.enums.EventsEnum.EVENT_PROCESSING
import br.com.windel.pos.enums.EventsEnum.EVENT_SUCCESS
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit


class PaymentGateway {
    lateinit var socket: Socket
    private lateinit var onConnect: Emitter.Listener
    private lateinit var onConnectError: Emitter.Listener
    private lateinit var onPay: Emitter.Listener
    val serialNumber = SERIAL

    constructor() {
        try {
            val connectionPool = ConnectionPool(5, 1, TimeUnit.DAYS)

            val okHttpClient = OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.DAYS)
                .connectTimeout(1, TimeUnit.DAYS)
                .callTimeout(1, TimeUnit.DAYS)
                .writeTimeout(1, TimeUnit.DAYS)
                .connectionPool(connectionPool)
                .build()

            val options = IO.Options()
            options.forceNew = true
            options.reconnection = true
            options.callFactory = okHttpClient
            options.webSocketFactory = okHttpClient

            val tokenJson = JsonObject();
            tokenJson.addProperty("clientTerminal", serialNumber)
            tokenJson.addProperty("apiKey", BuildConfig.WINDEL_POS_API_KEY)

            val tokenBase64 =
                Base64.encode(tokenJson.toString().toByteArray((Charsets.UTF_8)), Base64.DEFAULT)
            options.auth = mapOf(
                "token" to String(tokenBase64, Charsets.UTF_8),
            )

            socket = IO.socket(BuildConfig.WINDEL_POS_HOST, options)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }
    fun connect() {
        socket.connect()

        socket.on(Socket.EVENT_CONNECT, this.onConnect)
        socket.on(Socket.EVENT_CONNECT_ERROR, this.onConnectError)
        socket.on(EVENT_PAY.value, this.onPay)
    }

    fun disconnect() {
        socket.disconnect()
    }

    fun onConnectError(event: Emitter.Listener) {
        this.onConnectError = event
    }

    fun onConnectSuccess(event: Emitter.Listener) {
        this.onConnect = event
    }

    fun onReceivePay(onPay: Emitter.Listener) {
        this.onPay = onPay;
    }

    fun sendOnProccessing(data: String) {
        socket.emit(EVENT_PROCESSING.value, data)
    }

    fun sendOnSuccess(data: String) {
        socket.emit(EVENT_SUCCESS.value, data)
    }

    fun sendOnFailed(data: String) {
        socket.emit(EVENT_FAILED.value, data)
    }

    fun sendOnCanceled(data: String) {
        socket.emit(EVENT_CANCELED.value, data)
    }
}
