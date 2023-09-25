package br.com.windel.pay.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import br.com.windel.pay.Utils
import br.com.windel.pay.data.DataPayment
import br.com.windel.pay.data.DataPaymentVero
import br.com.windel.pay.enums.ItentEnum.VERO_PACKAGE
import br.com.windel.pay.enums.TransactionResponseEnum.CANCELED
import br.com.windel.pay.enums.TransactionResponseEnum.FAILED
import br.com.windel.pay.enums.TransactionResponseEnum.OK

class PaymentContract : ActivityResultContract<DataPayment, DataPaymentVero>() {
    private val veroIntentPagar: String = VERO_PACKAGE.value
    private val valorTransacao: String = "VALOR"

    override fun createIntent(context: Context, transactionData: DataPayment): Intent {
        val intent = Intent(veroIntentPagar)

        intent.putExtra("TRANSACAO", transactionData.transactionType)

        if (transactionData.transactionValue.isNotEmpty()){
            intent.putExtra(valorTransacao, Utils().convertToIntVero(transactionData.transactionValue))
            intent.putExtra("VALOR_TRANSACAO", Utils().convertToIntVero(transactionData.transactionValue))
        }

        if(transactionData.installments != null){
            intent.putExtra("NUMPARCELAS", transactionData.installments)
        }

        intent.putExtra("IMPRIMIR_COMPROVANTE", true)

        return intent
    }

    override fun parseResult(resultCode: Int, data: Intent?): DataPaymentVero {

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getStringExtra("AUTORIZACAO") == null) {
               return DataPaymentVero(FAILED.value, data.getStringExtra("ERRO"))
            }

            return DataPaymentVero(OK.value, data.dataString)
        }else{
            return DataPaymentVero(CANCELED.value, null)
        }
    }
}
