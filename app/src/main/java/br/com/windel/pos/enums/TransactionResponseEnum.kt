package br.com.windel.pos.enums

enum class TransactionResponseEnum(val value: String) {
    OK("OK"),
    CANCELED("CANCELED"),
    FAILED("FAILED")
}