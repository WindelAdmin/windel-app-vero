package br.com.windel.pay

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import br.com.windel.pay.contracts.PaymentContract


class MainActivity : AppCompatActivity() {
    private val TAG = "PaymentActivity"
    private lateinit var buttonCreatePayment: Button
    private lateinit var buttonCancel: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val pagamentoContract = registerForActivityResult(PaymentContract()) { pagamentoRealizado ->
            if (pagamentoRealizado.status === "OK") {
                // A ação foi realizada com sucesso
            } else {
                // A ação falhou ou foi cancelada
            }
        }

        buttonCreatePayment = findViewById(R.id.btnFinish);
        buttonCancel = findViewById(R.id.btnCancel);
        buttonCreatePayment.setOnClickListener {

            try {
                val valor = 100
                val transacao = "DEBITO";
                pagamentoContract.launch(Pair(valor, transacao))
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
        }

        buttonCancel.setOnClickListener{
            finish()
        }
    }
}