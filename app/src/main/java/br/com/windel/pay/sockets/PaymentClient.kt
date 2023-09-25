import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.net.URISyntaxException
import br.com.windel.pay.enums.EventsEnum.EVENT_PAY
import br.com.windel.pay.enums.EventsEnum.EVENT_PROCESSING
import br.com.windel.pay.enums.EventsEnum.EVENT_SUCCESS
import br.com.windel.pay.enums.EventsEnum.EVENT_FAILED
import br.com.windel.pay.enums.EventsEnum.EVENT_CANCELED
class PaymentClient {
    private lateinit var socket: Socket
    private lateinit var onConnect: Emitter.Listener
    private lateinit var onPay: Emitter.Listener

    init {
        try {
            val options = IO.Options()
            options.forceNew = true
            options.reconnection = true
            options.auth = mapOf(
                "token" to "terminal1"
            )

            socket = IO.socket("http://192.168.1.50:8080/payments", options)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connect() {

        socket.connect()

        //DEFAULT EVENTS
        socket.on(Socket.EVENT_CONNECT, this.onConnect)
        socket.on(Socket.EVENT_DISCONNECT){
            println("Desconectado do servidor WebSocket")
        }

        //PAYMENT_EVENTS
        socket.on(EVENT_PAY.value, this.onPay)
    }

    fun disconnect() {
        socket.disconnect()
    }

    fun onConnectionEstabilshed(event: Emitter.Listener){
        this.onConnect = event
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
