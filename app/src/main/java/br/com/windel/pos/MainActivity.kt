package br.com.windel.pos

import PaymentGateway
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.com.execucao.posmp_api.connection.Connectivity
import br.com.windel.pos.contracts.PaymentContract
import br.com.windel.pos.data.DataPayment
import br.com.windel.pos.data.DataPaymentResponse
import br.com.windel.pos.data.PaymentEntity
import br.com.windel.pos.data.TransactionData
import br.com.windel.pos.database.AppDatabase
import br.com.windel.pos.enums.ErrorEnum.CONNECTION_ERROR
import br.com.windel.pos.enums.ErrorEnum.SERVER_ERROR
import br.com.windel.pos.enums.EventsEnum.EVENT_FAILED
import br.com.windel.pos.enums.EventsEnum.EVENT_PROCESSING
import br.com.windel.pos.enums.EventsEnum.EVENT_SUCCESS
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {
    private lateinit var buttonCancel: Button
    private lateinit var lblStatus: TextView
    private lateinit var paymentGateway: PaymentGateway
    private lateinit var currentOrderId: String
    private lateinit var lottieAnimationView: LottieAnimationView
    private var socketIsConnected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()
        window.setFlags(
            FLAG_FULLSCREEN,
            FLAG_FULLSCREEN
        )

        val connectivity = Connectivity(this)
        connectivity.setAutomaticProxy(true)
        connectivity.checkProxy()
        connectivity.setProxy(true)
        connectivity.enable();

        lblStatus = findViewById(R.id.lblStatus);
        buttonCancel = findViewById(R.id.btnCancel);
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        lottieAnimationView.setAnimation(R.raw.loading_load)
        lottieAnimationView.playAnimation()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "payments-backup"
        ).build()

        val paymentContract = registerForActivityResult(PaymentContract()) { data ->

            if(socketIsConnected) {
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
            }else{
                val paymentModel = PaymentEntity(
                    0,
                    data.status,
                    data.data?.terminalSerial,
                    data.data?.flag,
                    data.data?.transactionType,
                    data.data?.authorization,
                    data.data?.nsu,
                    currentOrderId,
                    data.data?.error)

                lifecycleScope.launch {
                    try {
                        db.paymentDao().insert(paymentModel)
                    }catch (err: Exception){
                        lblStatus.text = err.message.toString()
                    }
                }
            }
        }


        paymentGateway = PaymentGateway();

        paymentGateway.onConnectSuccess {
            socketIsConnected = true

            lifecycleScope.launch {
                val paymentsPendingToReturn = db.paymentDao().getAll()

                if(paymentsPendingToReturn.isNotEmpty()){
                    paymentsPendingToReturn.forEach { it

                        val data = DataPaymentResponse(
                            it.status,
                            TransactionData(
                                it?.terminalSerial,
                                it?.flag,
                                it?.transactionType,
                                it?.authorization,
                                it?.nsu,
                                it?.orderId,
                                it?.error)
                        )

                        if (it.status == EVENT_SUCCESS.value) {
                            paymentGateway.sendOnSuccess(Gson().toJson(data))
                        } else if (it.status == EVENT_FAILED.value) {
                            paymentGateway.sendOnFailed(Gson().toJson(data))
                        }else {
                            paymentGateway.sendOnCanceled(Gson().toJson(data))
                        }
                        db.paymentDao().delete(it)
                    }
                }
            }

            Thread.sleep(500)
            runOnUiThread {
                Handler(Looper.getMainLooper()).postDelayed({
                        lottieAnimationView.setAnimation(R.raw.loading_success)
                        lottieAnimationView.resumeAnimation()
                        lblStatus.text = "Conexão estabelecida"
                        lblStatus.setTextColor(Color.parseColor("#1e873a"))

                    Handler(Looper.getMainLooper()).postDelayed({
                            lottieAnimationView.setAnimation(R.raw.loading_load)
                            lottieAnimationView.resumeAnimation()
                            lblStatus.text = "Aguardando Pedido de Pagamento"
                            lblStatus.setTextColor(Color.parseColor("#373737"))
                    }, 600)
                }, 500)
            }
        }

        paymentGateway.onConnectError{
            socketIsConnected = false
            Thread.sleep(500)
            runOnUiThread {
                lottieAnimationView.setAnimation(R.raw.loading_failed)
                lottieAnimationView.resumeAnimation()
                lblStatus.text =  if (checkInternetConnection()) SERVER_ERROR.value else CONNECTION_ERROR.value
                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
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
            runOnUiThread {
                lblStatus.text = "Aguardando conexão..."
                lblStatus.setTextColor(Color.parseColor("#373737"))
                lottieAnimationView.setAnimation(R.raw.loading_load)
                lottieAnimationView.resumeAnimation()
            }
            return true
        } else {
            runOnUiThread {
                lblStatus.text = CONNECTION_ERROR.value
                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
                lottieAnimationView.setAnimation(R.raw.loading_failed)
                lottieAnimationView.resumeAnimation()
            }
            return false
        }
    }
}