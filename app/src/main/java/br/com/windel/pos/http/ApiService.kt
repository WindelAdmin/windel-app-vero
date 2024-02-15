package br.com.windel.pos.http

import android.os.Build
import android.util.Log
import br.com.windel.pos.BuildConfig
import br.com.windel.pos.data.dtos.DataPaymentResponse
import br.com.windel.pos.enums.EndpointEnum
import br.com.windel.pos.gateways.BasicAuthInterceptor
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ApiService {
    private var httpClient = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor("d2luZGVsdXNlcg==", "dzFuZDNsQEAyMzIw")).build()

    fun findPayment(callback: Callback) {
        val request = Request.Builder()
            .url("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/terminal/${Build.SERIAL}")
            .build()

        try {
            httpClient.newCall(request).enqueue(callback)
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
    }

    fun sendProccessingPayment(orderId: String) {
        try {
            val requestProcessing = Request.Builder()
                .url("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/order/${orderId}/processing")
                .build()

            httpClient.newCall(requestProcessing).execute()
        } catch (e: IOException) {
            Log.e(this.javaClass.name, e.message.toString())
        }

    }

    fun sendSuccessPayment(dataPayment: DataPaymentResponse): Response? {
        try {
            val request = Request.Builder()
                .url("${EndpointEnum.GATEWAY_VERO_ORDER.value}/${dataPayment.data?.orderId}/payed")
                .post(
                    FormBody.Builder()
                        .add("terminalSerial", dataPayment.data?.terminalSerial.orEmpty())
                        .add("flag", dataPayment.data?.flag.orEmpty())
                        .add(
                            "transactionType",
                            dataPayment.data?.transactionType.orEmpty()
                        )
                        .add("authorization", dataPayment.data?.authorization.orEmpty())
                        .add("nsu", dataPayment.data?.nsu.orEmpty())
                        .add("orderId", dataPayment.data?.orderId.orEmpty())
                        .build()
                )
                .build()

            return httpClient.newCall(request).execute()
        } catch (e: IOException) {
            Log.e(this.javaClass.name, e.message.toString())
            return null
        }
    }

     fun sendCanceledPayment(orderId: String): Response? {
        try {
            val request = Request.Builder()
                .url("${EndpointEnum.GATEWAY_VERO_ORDER.value}/${orderId}/canceled")
                .build()

            return httpClient.newCall(request).execute()
        } catch (e: IOException) {
            Log.e(this.javaClass.name, e.message.toString())
            return null
        }
    }

     fun sendFailedPayment(error: String, orderId: String): Response? {
        try {
            val request = Request.Builder()
                .url("${EndpointEnum.GATEWAY_VERO_ORDER.value}/${orderId}/failed")
                .post(
                    FormBody.Builder().add("error", error)
                        .build()
                )
                .build()

            return httpClient.newCall(request).execute()
        } catch (e: IOException) {
            Log.e(this.javaClass.name, e.message.toString())
            return null
        }
    }
}