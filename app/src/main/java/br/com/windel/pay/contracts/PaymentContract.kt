package br.com.windel.pay.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import br.com.windel.pay.Utils
import br.com.windel.pay.data.DataPayment
import br.com.windel.pay.data.DataPaymentVero
import br.com.windel.pay.enums.ItentEnum.VERO_PAGAR

class PaymentContract : ActivityResultContract<DataPayment, DataPaymentVero>() {
    private val veroIntentPagar: String = VERO_PAGAR.value
    private val valorTransacao: String = "VALOR"

    override fun createIntent(context: Context, input: DataPayment): Intent {
        val intent = Intent(veroIntentPagar)

        if (input.transactionValue.isNotEmpty()){
            intent.putExtra(valorTransacao, Utils().convertToIntVero(input.transactionValue))
        }

        intent.putExtra("TRANSACAO", input.transactionType)
        return intent
    }

    override fun parseResult(resultCode: Int, data: Intent?): DataPaymentVero {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val auth = data.getStringExtra("AUTORIZACAO")
            if (auth == null) {
               return DataPaymentVero("DECLINADA", null)
            }
            return DataPaymentVero("OK", data.getStringExtra("VALOR"))
        }else{
            return DataPaymentVero(status = "ERROR", null)
        }
    }
}
