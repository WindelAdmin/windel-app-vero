package br.com.windel.pos

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build.SERIAL
import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.com.execucao.posmp_api.connection.Connectivity
import br.com.windel.pos.contracts.PaymentContract
import br.com.windel.pos.data.dtos.DataPayment
import br.com.windel.pos.data.dtos.DataPaymentResponse
import br.com.windel.pos.data.dtos.TransactionData
import br.com.windel.pos.data.entities.PaymentEntity
import br.com.windel.pos.database.AppDatabase
import br.com.windel.pos.enums.ErrorEnum
import br.com.windel.pos.enums.EventsEnum.EVENT_CANCELED
import br.com.windel.pos.enums.EventsEnum.EVENT_SUCCESS
import br.com.windel.pos.gateways.BasicAuthInterceptor
import br.com.windel.pos.http.ApiService
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var buttonCancel: Button
    private lateinit var buttonSettings: ImageButton
    private lateinit var lblStatus: TextView
    private lateinit var currentOrderId: String
    private lateinit var lottieAnimationView: LottieAnimationView
    private var httpClient = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor("d2luZGVsdXNlcg==", "dzFuZDNsQEAyMzIw")).build()

    private lateinit var paymentContract: ActivityResultLauncher<DataPayment>
    private lateinit var context: Context
    private var manualMode: Boolean = false
    private val paymentService = ApiService()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()
        httpClient.dispatcher.maxRequests = 1

        window.setFlags(
            FLAG_FULLSCREEN,
            FLAG_FULLSCREEN
        )

        context = this
        currentOrderId = ""

        lblStatus = findViewById(R.id.lblStatus);
        lblStatus.text = ""
        buttonCancel = findViewById(R.id.buttonExit);
        buttonSettings = findViewById(R.id.buttonSettings)
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        lottieAnimationView.setAnimation(R.raw.loading_load)
        lottieAnimationView.playAnimation()

        buttonCancel.setOnClickListener {
            finish()
        }

        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, 1)
        }

        val connectivity = Connectivity(this)
        connectivity.setAutomaticProxy(true)
        connectivity.checkProxy()
        connectivity.setProxy(true)
        connectivity.enable();




        checkModeManual()

        paymentContract = registerForActivityResult(PaymentContract()) { data ->

            if (checkInternetConnection()) {
                data?.data?.terminalSerial = SERIAL
                data?.data?.orderId = currentOrderId

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        val response = if (data.status === EVENT_SUCCESS.value)
                            paymentService.sendSuccessPayment(data)
                        else if (data.status === EVENT_CANCELED.value)
                            paymentService.sendCanceledPayment(currentOrderId)
                        else
                            paymentService.sendFailedPayment(
                                data.data?.error.orEmpty(),
                                currentOrderId
                            )

                        if (response !== null && response.isSuccessful) {
                            if (!manualMode) checkPayments()
                        } else {
                            runOnUiThread {
                                lblStatus.text = ErrorEnum.SERVER_ERROR.value
                                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
                            }
                            if (!manualMode) checkPayments()
                        }

                        response?.close()
                    }
                }
            } else {
                saveOnDatabase(data)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                assertPayment()
            }
        }
    }

    private fun setLottieToManualModel() {
        runOnUiThread {
            val layoutParams = lottieAnimationView.layoutParams
            lottieAnimationView.setAnimation(R.raw.sync_idle)
            lottieAnimationView.playAnimation()
            layoutParams.width = 350
            layoutParams.height = 350
            lottieAnimationView.layoutParams = layoutParams
            lblStatus.text = ""
            lblStatus.setTextColor(Color.parseColor("#373737"))
        }
    }

    private fun checkPayments() {

        Thread.sleep(3000)

        if (checkInternetConnection()) {
            paymentService.findPayment(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!manualMode) checkPayments() else setLottieToManualModel()
                    e.printStackTrace()
                    call.cancel()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) throw IOException ("Requisição mal sucedida: $response")

                    try {
                        if (response.body.contentLength() == 0L) {
                            response.close()
                            call.cancel()
                            if (!manualMode) checkPayments() else setLottieToManualModel()
                            return
                        }

                        val data = Gson().fromJson(response.body.string(), DataPayment::class.java)

                        response.close()
                        call.cancel()

                        currentOrderId = data.orderId

                        if (data.status == "processando") {
                            openPaymentProcessing(data)
                            return
                        }

                        paymentService.sendProccessingPayment(data.orderId)

                        paymentContract.launch(data)

                        if(manualMode) {
                            setLottieToManualModel()
                        }
                    } catch (e: Exception) {
                        Log.e(this.javaClass.name, e.message.toString())
                    }
                }
            })
        } else {
            runOnUiThread {
                lblStatus.text = ErrorEnum.CONNECTION_ERROR.value
                lblStatus.setTextColor(Color.parseColor("#a31a1a"))
                if (!manualMode) checkPayments() else setLottieToManualModel()
            }
        }
    }

    private suspend fun assertPayment() {
        try {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "payments-backup"
            ).build()
            val paymentsPendingToReturn = db.paymentDao().getAll()

            if (paymentsPendingToReturn.isNotEmpty()) {
                paymentsPendingToReturn.forEach {
                    it

                    val data = DataPaymentResponse(
                        it.status,
                        TransactionData(
                            it?.terminalSerial,
                            it?.flag,
                            it?.transactionType,
                            it?.authorization,
                            it?.nsu,
                            it?.orderId,
                            it?.error
                        )
                    )

                    val response = if (data.status === EVENT_SUCCESS.value)
                        paymentService.sendSuccessPayment(data)
                    else if (data.status === EVENT_CANCELED.value)
                        paymentService.sendCanceledPayment(currentOrderId)
                    else
                        paymentService.sendFailedPayment(data.data?.error.orEmpty(), currentOrderId)

                    if (response !== null && response.isSuccessful) {
                        if (!manualMode) checkPayments() else setLottieToManualModel()
                    }

                    response?.close()
                    db.paymentDao().delete(it)
                }
            }
        } catch (e: IOException) {
            runOnUiThread {
                lblStatus.text = e.message.toString()
            }
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    private fun saveOnDatabase(data: DataPaymentResponse) {

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "app"
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
            data.data?.error
        )

        lifecycleScope.launch {
            try {
                db.paymentDao().insert(paymentModel)
                if (!manualMode) checkPayments() else setLottieToManualModel()
            } catch (e: Exception) {
                runOnUiThread {
                    if (!manualMode) checkPayments() else setLottieToManualModel()
                    lblStatus.text = e.message.toString()
                    Log.e(this.javaClass.name, e.message.toString())
                }
            }
        }
    }

    private fun checkInternetConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun openPaymentProcessing(data: DataPayment) {
        runOnUiThread {
            val builder = AlertDialog.Builder(context)
            builder.setCancelable(false)
            builder.setTitle("Ops!")
            builder.setMessage("Você possui um pagamento em aberto, o que deseja fazer?")
            builder.setPositiveButton("Refazer Pagamento") { dialog, which ->
                paymentContract.launch(data)
            }

            builder.setNegativeButton("Cancelar") { dialog, which ->
                dialog.cancel()

                val progressDialog = ProgressDialog(context)
                progressDialog?.setMessage("Loading...")
                progressDialog?.setCancelable(false)
                progressDialog?.show()

                try {

                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.IO) {
                           val response =  paymentService.sendCanceledPayment(currentOrderId)

                            if(response !== null && response.isSuccessful) {
                                if (!manualMode) checkPayments() else setLottieToManualModel()
                            }

                            progressDialog.dismiss()
                        }
                    }
                }  catch (e: Exception) {
                    Log.e(this.javaClass.name, e.message.toString())
                }
            }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        checkModeManual()
    }

    fun checkModeManual() {
        val sharedPreferences = getSharedPreferences("windelConfig", Context.MODE_PRIVATE)
        manualMode = sharedPreferences.getBoolean("manualMode", false)
        val layoutParams = lottieAnimationView.layoutParams

        if (!manualMode) {
            runOnUiThread {
                lottieAnimationView.setAnimation(R.raw.loading_load)
                lottieAnimationView.playAnimation()
                layoutParams.width = 265
                layoutParams.height = 225
                lottieAnimationView.layoutParams = layoutParams
                lblStatus.text = "Aguardando pedido de pagamento..."
            }

            checkPayments()
        } else {
            runOnUiThread {
                lblStatus.text = ""
                lottieAnimationView.setAnimation(R.raw.sync_idle)
                lottieAnimationView.playAnimation()
                layoutParams.width = 350
                layoutParams.height = 350
                lottieAnimationView.layoutParams = layoutParams
            }

            lottieAnimationView.setOnClickListener {

                runOnUiThread {
                    lottieAnimationView.setAnimation(R.raw.sync_loading)
                    lottieAnimationView.playAnimation()
                    lblStatus.text = "Buscando pagamento..."
                }

                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        checkPayments()
                    }
                }
            }
        }
    }
}