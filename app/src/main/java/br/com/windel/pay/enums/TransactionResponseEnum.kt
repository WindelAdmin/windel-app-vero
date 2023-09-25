package br.com.windel.pay.enums

enum class TransactionResponseEnum(val value: String) {
    OK("OK"),
    CANCELED("CANCELED"),
    FAILED("FAILED")
}