package br.com.windel.pos.data

data class DataPaymentResponse(val status: String?, var data: TransactionData?, val error: String?)

data class TransactionData(
    val terminalSerial: String?,
    val flag: String?,
    val transactionType: String?,
    val authorization: String?,
    val nsu: String?,
    var orderId: String?,
    val error: String?
)