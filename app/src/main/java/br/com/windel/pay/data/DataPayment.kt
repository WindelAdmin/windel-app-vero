package br.com.windel.pay.data

data class DataPayment(
    val transactionValue: String,
    val transactionType: String,
    val installments: Int?,
    val nsu: String?,
    val printVoucher: Boolean?)
