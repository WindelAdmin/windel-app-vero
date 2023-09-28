package br.com.windel.pos.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import br.com.windel.pos.Utils
import br.com.windel.pos.data.DataPayment
import br.com.windel.pos.data.DataPaymentResponse
import br.com.windel.pos.data.TransactionData
import br.com.windel.pos.enums.ItentEnum.VERO_PACKAGE
import br.com.windel.pos.enums.ItentEnum.TRANSACTION_VALUE_LABEL
import br.com.windel.pos.enums.ItentEnum.TRANSACTION_LABEL
import br.com.windel.pos.enums.ItentEnum.DAY_LABEL
import br.com.windel.pos.enums.ItentEnum.EXPIRATION_DATE_LABEL
import br.com.windel.pos.enums.ItentEnum.INSTALLMENTS_LABEL
import br.com.windel.pos.enums.ItentEnum.PRINT_VOUCHER
import br.com.windel.pos.enums.ItentEnum.AUTHORIZATION_LABEL
import br.com.windel.pos.enums.ItentEnum.SERIAL_LABEL
import br.com.windel.pos.enums.ItentEnum.FLAG_LABEL
import br.com.windel.pos.enums.ItentEnum.NSU_LABEL
import br.com.windel.pos.enums.ItentEnum.ERROR_LABEL
import br.com.windel.pos.enums.TransactionResponseEnum.CANCELED
import br.com.windel.pos.enums.TransactionResponseEnum.FAILED
import br.com.windel.pos.enums.TransactionResponseEnum.OK

class PaymentContract : ActivityResultContract<DataPayment, DataPaymentResponse>() {
    private val veroIntentPagar: String = VERO_PACKAGE.value

    override fun createIntent(context: Context, transactionData: DataPayment): Intent {
        val intent = Intent(veroIntentPagar)

        intent.putExtra(TRANSACTION_LABEL.value, transactionData.transactionType)

        if (transactionData.transactionValue.isNotEmpty()){
            intent.putExtra(TRANSACTION_VALUE_LABEL.value, Utils().convertToIntVero(transactionData.transactionValue))
        }

        transactionData.installments?.let {
            intent.putExtra(INSTALLMENTS_LABEL.value, it)
        }

        transactionData.dayOfMonth?.let {
            intent.putExtra(DAY_LABEL.value, it)
        }

        transactionData.expirationDate?.let {
            intent.putExtra(EXPIRATION_DATE_LABEL.value, it)
        }

        intent.putExtra(PRINT_VOUCHER.value, true)

        return intent
    }

    override fun parseResult(resultCode: Int, data: Intent?): DataPaymentResponse {

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getStringExtra(AUTHORIZATION_LABEL.value) == null) {
               return DataPaymentResponse(FAILED.value, null, data.getStringExtra(ERROR_LABEL.value))
            }

            return DataPaymentResponse(OK.value, extractTransactionDataFromIntent(data), data.getStringExtra(ERROR_LABEL.value))
        }else{
            return DataPaymentResponse(CANCELED.value, null, data?.getStringExtra(ERROR_LABEL.value))
        }
    }

    private fun extractTransactionDataFromIntent(intent: Intent?): TransactionData? {
        if (intent == null) {1
            return null
        }

        val SERIAL = intent.getStringExtra(SERIAL_LABEL.value) ?: ""
        val BANDEIRA = intent.getStringExtra(FLAG_LABEL.value) ?: ""
        val TRANSACAO = intent.getStringExtra(TRANSACTION_LABEL.value) ?: ""
        val AUTORIZACAO = intent.getStringExtra(AUTHORIZATION_LABEL.value) ?: ""
        val NSU = intent.getStringExtra(NSU_LABEL.value) ?: ""


        val ERRO = intent.getStringExtra(ERROR_LABEL.value) ?: null

        return TransactionData(
            SERIAL,
            BANDEIRA,
            TRANSACAO,
            AUTORIZACAO,
            NSU,
            null,
            ERRO
        )
    }
}
