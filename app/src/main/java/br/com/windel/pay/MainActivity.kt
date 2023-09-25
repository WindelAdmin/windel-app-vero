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
import br.com.windel.pay.enums.TransactionResponseEnum
import br.com.windel.pay.enums.TransactionResponseEnum.FAILED
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
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

        val paymentContract = registerForActivityResult(PaymentContract()) { data ->
            if (data.status === "OK") {
                paymentClient.sendOnSuccess("transação concluída")
            } else if(data.status == FAILED.value){
                paymentClient.sendOnFailed("transação declinada")
            }else{
                paymentClient.sendOnCanceled("transação cancelada")
            }
        }

        paymentClient = PaymentClient();
        paymentClient.onConnectionEstabilshed {
            Thread.sleep(200)
            runOnUiThread {
                Handler(Looper.getMainLooper()).postDelayed({
                    lblStatus.text = "Conexão estabelecida"
                    lblStatus.setTextColor(Color.parseColor("#1e873a"))

                    Handler(Looper.getMainLooper()).postDelayed({
                        lblStatus.text = "Aguardando Pedido de Pagamento"
                        lblStatus.setTextColor(Color.parseColor("#373737"))
                    }, 300)
                }, 200)
            }
        }

        paymentClient.onDisconnect{
            Thread.sleep(500)
            runOnUiThread{
                lblStatus.text = "Erro de conexão, entre em contato com o suporte."
                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
            }
        }

        paymentClient.onReceivePay { args ->
            if (args.isNotEmpty()) {
                val data = Gson().fromJson(args[0] as String, DataPayment::class.java)
                try {
                    paymentClient.sendOnProccessing("transação processando...")
                    paymentContract.launch(data)
                } catch (e: Exception) {
                    Log.e(this.javaClass.name, e.message.toString())
                }
            }
        }

        paymentClient.connect();

        //UI EVENTS
        buttonCreatePayment.setOnClickListener {

            try {
                paymentContract.launch(DataPayment("", "123", 0, null, null))
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }

        buttonCancel.setOnClickListener {
            paymentClient.disconnect()
            finish()
        }
    }
}

private data class ClientID(val id: String)