package br.com.windel.pos

import PaymentGateway
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.windel.pos.contracts.PaymentContract
import br.com.windel.pos.data.DataPayment
import br.com.windel.pos.data.DataPaymentResponse
import br.com.windel.pos.data.TransactionData
import br.com.windel.pos.enums.ErrorEnum
import br.com.windel.pos.enums.ErrorEnum.CONNECTION_ERROR
import br.com.windel.pos.enums.ErrorEnum.SERVER_ERROR
import br.com.windel.pos.enums.EventsEnum.EVENT_FAILED
import br.com.windel.pos.enums.EventsEnum.EVENT_PROCESSING
import br.com.windel.pos.enums.EventsEnum.EVENT_SUCCESS
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    private lateinit var buttonCreatePayment: Button
    private lateinit var buttonCancel: Button
    private lateinit var lblStatus: TextView
    private lateinit var paymentGateway: PaymentGateway
    private lateinit var currentOrderId: String
    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()
        window.setFlags(
            FLAG_FULLSCREEN,
            FLAG_FULLSCREEN
        )

        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        lottieAnimationView.setAnimation(R.raw.loading_load)
        lottieAnimationView.playAnimation()

        //SET UI COMPONENTS
        lblStatus = findViewById(R.id.lblStatus);
        buttonCreatePayment = findViewById(R.id.btnFinish);
        buttonCancel = findViewById(R.id.btnCancel);

        val paymentContract = registerForActivityResult(PaymentContract()) { data ->
            if (data.status === EVENT_SUCCESS.value) {
                data.data?.orderId = currentOrderId
                paymentGateway.sendOnSuccess(Gson().toJson(data))
            } else if (data.status == EVENT_FAILED.value) {
                data?.data?.terminalSerial = paymentGateway.serialNumber
                data?.data?.orderId = currentOrderId
                paymentGateway.sendOnFailed(Gson().toJson(data))
            } else {
                data?.data?.terminalSerial = paymentGateway.serialNumber
                data?.data?.orderId = currentOrderId
                paymentGateway.sendOnCanceled(Gson().toJson(data))
            }
        }

        //SOCKET
        paymentGateway = PaymentGateway();

        paymentGateway.onConnectSuccess {
            Thread.sleep(500)
            runOnUiThread {
                Handler(Looper.getMainLooper()).postDelayed({
                    lottieAnimationView.setAnimation(R.raw.loading_success)
                    lottieAnimationView.resumeAnimation()

                    lblStatus.text = "Conexão estabelecida"
                    lblStatus.setTextColor(Color.parseColor("#1e873a"))

                    Handler(Looper.getMainLooper()).postDelayed({
                        lblStatus.text = "Aguardando Pedido de Pagamento"
                        lblStatus.setTextColor(Color.parseColor("#373737"))
                        runOnUiThread {
                            lottieAnimationView.setAnimation(R.raw.loading_load)
                            lottieAnimationView.resumeAnimation()
                        }
                    }, 600)
                }, 500)
            }
        }

        paymentGateway.onConnectError{
            Thread.sleep(500)
            runOnUiThread {
                lblStatus.text =  if (checkInternetConnection()) SERVER_ERROR.value else CONNECTION_ERROR.value
                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
                lottieAnimationView.setAnimation(R.raw.loading_failed)
                lottieAnimationView.resumeAnimation()

            }
        }

        paymentGateway.onReceivePay { args ->
            if (args.isNotEmpty()) {
                val data = Gson().fromJson(args[0] as String, DataPayment::class.java)
                currentOrderId = data.orderId

                try {
                    val transcationData = TransactionData()
                    transcationData.terminalSerial = paymentGateway.serialNumber
                    transcationData.orderId = currentOrderId

                    paymentGateway.sendOnProccessing(
                        Gson().toJson(
                            DataPaymentResponse(
                                EVENT_PROCESSING.value, transcationData
                            )
                        )
                    )

                    paymentContract.launch(data)
                } catch (e: Exception) {
                    Log.e(this.javaClass.name, e.message.toString())
                }
            }
        }

        paymentGateway.connect()

        //UI EVENTS
        buttonCreatePayment.setOnClickListener {
            try {
                paymentContract.launch(DataPayment("", "123", 0, null, null, null, ""))
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }

        buttonCancel.setOnClickListener {
            paymentGateway.disconnect()
            finish()
        }
    }

    private fun checkInternetConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkInfo = connectivityManager.activeNetworkInfo

        val isConnected = networkInfo != null && networkInfo.isConnected

        if (isConnected) {
            lblStatus.text = "Aguardando conexão..."
            lblStatus.setTextColor(Color.parseColor("#373737"))
            lottieAnimationView.setAnimation(R.raw.loading_load)
            lottieAnimationView.resumeAnimation()
            return true
        } else {
            lblStatus.text = CONNECTION_ERROR.value
            lblStatus.setTextColor(Color.parseColor("#a31a1a"))
            lottieAnimationView.setAnimation(R.raw.loading_failed)
            lottieAnimationView.resumeAnimation()
            return false
        }
    }
}