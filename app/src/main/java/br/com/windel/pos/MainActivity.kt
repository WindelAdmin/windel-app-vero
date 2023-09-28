package br.com.windel.pos

import PaymentGateway
import android.graphics.Color
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
import br.com.windel.pos.enums.TransactionResponseEnum.FAILED
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    private lateinit var buttonCreatePayment: Button
    private lateinit var buttonCancel: Button
    private lateinit var lblStatus: TextView
    private lateinit var paymentGateway: PaymentGateway
    private lateinit var currentOrderId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()
        window.setFlags(
            FLAG_FULLSCREEN,
            FLAG_FULLSCREEN
        )

        val lottieAnimationView: LottieAnimationView = findViewById(R.id.lottieAnimationView)

        lottieAnimationView.setAnimation(R.raw.loading_card_01)
        lottieAnimationView.playAnimation()


        //SET UI COMPONENTS
        lblStatus = findViewById(R.id.lblStatus);
        buttonCreatePayment = findViewById(R.id.btnFinish);
        buttonCancel = findViewById(R.id.btnCancel);

        val paymentContract = registerForActivityResult(PaymentContract()) { data ->
            if (data.status === "OK") {
                data.data?.orderId = currentOrderId
                paymentGateway.sendOnSuccess(Gson().toJson(data))
            } else if(data.status == FAILED.value){
                data.data = TransactionData(null, null, null, null, null,  currentOrderId,null)
                paymentGateway.sendOnFailed(Gson().toJson(data))
            }else{
                data.data = TransactionData(null, null, null, null, null,  currentOrderId,null)
                paymentGateway.sendOnCanceled(Gson().toJson(data))
            }
        }

        //SOCKET
        paymentGateway = PaymentGateway();
        paymentGateway.onConnectionEstabilshed {
           runOnUiThread{
               lottieAnimationView.setAnimation(R.raw.loading_card_03)
           }
            Thread.sleep(400)
            runOnUiThread {
                Handler(Looper.getMainLooper()).postDelayed({
                    lblStatus.text = "Conexão estabelecida"
                    lblStatus.setTextColor(Color.parseColor("#1e873a"))

                    Handler(Looper.getMainLooper()).postDelayed({
                        lblStatus.text = "Aguardando Pedido de Pagamento"
                        lblStatus.setTextColor(Color.parseColor("#373737"))
                        runOnUiThread {
                            lottieAnimationView.pauseAnimation()
                        }
                    }, 600)
                }, 400)
            }
        }

        paymentGateway.onDisconnect{

            Thread.sleep(800)
            runOnUiThread{
                lblStatus.text = "Erro de conexão, entre em contato com o suporte."
                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
                runOnUiThread {
                    lottieAnimationView.setAnimation(R.raw.loading_card_02)
                }
            }
        }

        paymentGateway.onReceivePay { args ->
            if (args.isNotEmpty()) {
                val data = Gson().fromJson(args[0] as String, DataPayment::class.java)
                try {
                    paymentGateway.sendOnProccessing(Gson().toJson(DataPaymentResponse("processando...", null, null)))
                    currentOrderId = data.orderId
                    paymentContract.launch(data)
                } catch (e: Exception) {
                    Log.e(this.javaClass.name, e.message.toString())
                }
            }
        }

        paymentGateway.connect();

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
}

private data class ClientID(val id: String)