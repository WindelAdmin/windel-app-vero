package br.com.windel.pay.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class PaymentContract : ActivityResultContract<Pair<Number, String>, DataPaymentVero>() {
    private val INTENT_PAGAR = "br.com.execucao.PAGAR"
    val VALOR_TRANSACAO = "VALOR"

    override fun createIntent(context: Context, input: Pair<Number, String>): Intent {
        val (valor, transacao) = input
        val intent = Intent(INTENT_PAGAR)
        intent.putExtra(VALOR_TRANSACAO, valor)
        intent.putExtra("TRANSACAO", transacao)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): DataPaymentVero {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            val status = intent.getStringExtra("status")
            return DataPaymentVero(status)
        }else{
            return DataPaymentVero(status = "ERROR")
        }
    }
}
