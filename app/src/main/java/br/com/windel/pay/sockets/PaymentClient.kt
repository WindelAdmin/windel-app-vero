import android.os.Build.SERIAL
import android.util.Base64
import br.com.windel.pay.BuildConfig.WINDEL_PAY_HOST
import br.com.windel.pay.BuildConfig.WINDEL_PAY_API_KEY
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.net.URISyntaxException
import br.com.windel.pay.enums.EventsEnum.EVENT_PAY
import br.com.windel.pay.enums.EventsEnum.EVENT_PROCESSING
import br.com.windel.pay.enums.EventsEnum.EVENT_SUCCESS
import br.com.windel.pay.enums.EventsEnum.EVENT_FAILED
import br.com.windel.pay.enums.EventsEnum.EVENT_CANCELED
import com.google.gson.JsonObject


class PaymentClient {
    private lateinit var socket: Socket
    private lateinit var onConnect: Emitter.Listener
    private lateinit var onDisconnect: Emitter.Listener
    private lateinit var onPay: Emitter.Listener
    val serialNumber = SERIAL
    init {
        try {
            val options = IO.Options()
            options.forceNew = true
            options.reconnection = true
            val tokenJson =  JsonObject();
            tokenJson.addProperty("clientTerminal", serialNumber)
            tokenJson.addProperty("apiKey", WINDEL_PAY_API_KEY)
            val tokenBase64 = Base64.encode(tokenJson.toString().toByteArray((Charsets.UTF_8)), Base64.DEFAULT)
            options.auth = mapOf(
                "token" to String(tokenBase64, Charsets.UTF_8),
            )

            socket = IO.socket( WINDEL_PAY_HOST, options)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connect() {

        socket.connect()

        //DEFAULT EVENTS
        socket.on(Socket.EVENT_CONNECT, this.onConnect)
        socket.on(Socket.EVENT_DISCONNECT, this.onDisconnect)

        //PAYMENT_EVENTS
        socket.on(EVENT_PAY.value, this.onPay)
    }

    fun disconnect() {
        socket.disconnect()
    }

    fun onConnectionEstabilshed(event: Emitter.Listener){
        this.onConnect = event
    }

    fun onDisconnect(event: Emitter.Listener){
        this.onDisconnect = event
    }

    fun onReceivePay(onPay: Emitter.Listener){
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
