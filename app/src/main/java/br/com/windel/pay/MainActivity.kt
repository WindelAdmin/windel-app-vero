package br.com.windel.pay

import PaymentClient
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.windel.pay.contracts.PaymentContract
import br.com.windel.pay.data.DataPayment
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    private val TAG = "PaymentActivity"
    private lateinit var buttonCreatePayment: Button
    private lateinit var buttonCancel: Button
    private lateinit var lblStatus: TextView
    private lateinit var paymentClient: PaymentClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        lblStatus = findViewById(R.id.lblStatus);
        buttonCreatePayment = findViewById(R.id.btnFinish);
        buttonCancel = findViewById(R.id.btnCancel);

        runOnUiThread {
            lblStatus.text = "Estabelecendo conexão..."
        }

        val pagamentoContract = registerForActivityResult(PaymentContract()) { data ->
            if (data.status === "OK") {
                lblStatus.text = data.transactionValue
            } else if(data.status === "DECLINADA"){
                paymentClient.sendOnFailed("transação declinada")
            }else{
                paymentClient.sendOnCanceled("transação cancelada")
            }
        }


        paymentClient = PaymentClient();
        paymentClient.onConnectionEstabilshed {
            Thread.sleep(500)
            runOnUiThread {
                Handler(Looper.getMainLooper()).postDelayed({
                    lblStatus.text = "Conexão estabelecida"
                    lblStatus.setTextColor(Color.parseColor("#1e873a"))

                    Handler(Looper.getMainLooper()).postDelayed({
                        lblStatus.text = "Aguardando Pedido de Pagamento"
                        lblStatus.setTextColor(Color.parseColor("#373737"))
                    }, 1500)
                }, 500)
            }
        }
        paymentClient.onReceivePay { args ->
            if (args.isNotEmpty()) {
                val data = Gson().fromJson(args[0] as String, DataPayment::class.java)
                try {
                    paymentClient.sendOnProccessing("transação processando...")
                   pagamentoContract.launch(data)
                } catch (e: Exception) {
                    Log.e(TAG, e.message.toString())
                }
            }
        }

        paymentClient.connect();

        //UI EVENTS
        buttonCreatePayment.setOnClickListener {

            try {
                pagamentoContract.launch(DataPayment("", "123"))
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
        }

        buttonCancel.setOnClickListener {
            paymentClient.disconnect()
            finish()
        }
    }
}

private data class ClientID(val id: String)