package br.com.windel.pos.data

data class DataPayment(
    val transactionValue: String?,
    val transactionType: String,
    val installments: Int?,
    val nsu: String?,
    val dayOfMonth: Int?,
    val expirationDate: Int?,
    val orderId: String,
    val status: String
)
