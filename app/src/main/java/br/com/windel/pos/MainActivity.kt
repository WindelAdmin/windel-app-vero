package br.com.windel.pos

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build.SERIAL
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
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
import br.com.windel.pos.enums.EventsEnum.EVENT_SUCCESS
import br.com.windel.pos.gateways.BasicAuthInterceptor
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var buttonCancel: Button
    private lateinit var lblStatus: TextView
    private lateinit var currentOrderId: String
    private lateinit var lottieAnimationView: LottieAnimationView
    private var httpClient = OkHttpClient.Builder().addInterceptor(BasicAuthInterceptor("d2luZGVsdXNlcg==", "dzFuZDNsQEAyMzIw")).build()

    private lateinit var paymentContract : ActivityResultLauncher<DataPayment>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()

        window.setFlags(
            FLAG_FULLSCREEN,
            FLAG_FULLSCREEN
        )

        currentOrderId = ""

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

        checkPayments();

            paymentContract = registerForActivityResult(PaymentContract()) { data ->

            if(checkInternetConnection()) {
                if (data.status === EVENT_SUCCESS.value) {
                    data.data?.orderId = currentOrderId
                } else if (data.status == EVENT_FAILED.value) {
                    data?.data?.terminalSerial = SERIAL
                    data?.data?.orderId = currentOrderId
                } else {
                    data?.data?.terminalSerial = SERIAL
                    data?.data?.orderId = currentOrderId
                }

                sendResultPayment(data)
            }else{
                saveOnDatabase(data)
            }
        }

        buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun checkPayments() {
        Thread.sleep(3000)
        if(checkInternetConnection()) {
            val request = Request.Builder()
                .url("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/terminal/${SERIAL}")
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        lottieAnimationView.setAnimation(R.raw.loading_failed)
                        lottieAnimationView.resumeAnimation()
                        lblStatus.text = SERVER_ERROR.value
                        lblStatus.setTextColor(Color.parseColor("#a31a1a"))

                        checkPayments()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    try {
                        if(response.body.contentLength() == 0L)  {
                            checkPayments()
                            return
                        }

                        val data = Gson().fromJson(response.body.string(), DataPayment::class.java)

                        currentOrderId = data.orderId

                        val transcationData = TransactionData()
                        transcationData.terminalSerial = SERIAL
                        transcationData.orderId = currentOrderId

                        val requestProcessing = Request.Builder()
                            .url("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/order/${data.orderId}/processing")
                            .build()
                        httpClient.newCall(requestProcessing).execute()

                        paymentContract.launch(data)
                    } catch (e: Exception) {
                        Log.e(this.javaClass.name, e.message.toString())
                    }
                }
            })
        } else {
            checkPayments()
        }
    }

    private fun sendResultPayment(dataPayment: DataPaymentResponse) {
        try {
            val request =
                if (dataPayment.status === "success")
                    Request.Builder()
                        .url("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/order/${dataPayment.data?.orderId}/payed")
                        .post(FormBody.Builder()
                            .add("terminalSerial", dataPayment?.data?.terminalSerial.orEmpty())
                            .add("flag", dataPayment?.data?.flag.orEmpty())
                            .add("transactionType", dataPayment?.data?.transactionType.orEmpty())
                            .add("authorization", dataPayment?.data?.authorization.orEmpty())
                            .add("nsu", dataPayment?.data?.nsu.orEmpty())
                            .add("orderId", dataPayment?.data?.orderId.orEmpty())
                            .build())
                        .build()
                else
                    Request.Builder()
                        .url("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/order/${currentOrderId}/${dataPayment.status}")
                        .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    checkPayments()
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        checkPayments()
                        throw IOException("Unexpected code $response")
                    }
                   checkPayments()
                }
            })
        } catch (e: IOException) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    private fun saveOnDatabase(data: DataPaymentResponse) {

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "payments-backup"
        ).build()

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
                checkPayments()
            }catch (e: Exception){
                lblStatus.text = e.message.toString()
                Log.e(this.javaClass.name, e.message.toString())
            }
        }
    }


    private fun checkInternetConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkInfo = connectivityManager.activeNetworkInfo

        val isConnected = networkInfo != null && networkInfo.isConnected

        if (isConnected) {
            runOnUiThread {
                lblStatus.text = "Aguardando Pedido de Pagamento"
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