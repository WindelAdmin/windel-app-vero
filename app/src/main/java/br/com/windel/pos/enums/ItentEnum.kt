package br.com.windel.pos.enums

enum class ItentEnum (val value: String){
    VERO_PACKAGE("br.com.execucao.PAGAR"),
    TRANSACTION_VALUE_LABEL("VALOR"),
    TRANSACTION_LABEL("TRANSACAO"),
    INSTALLMENTS_LABEL("NUMPARCELAS"),
    DAY_LABEL("DIA"),
    EXPIRATION_DATE_LABEL("PRAZO"),
    PRINT_VOUCHER("IMPRIMIR_COMPROVANTE"),

    AUTHORIZATION_LABEL("AUTORIZACAO"),
    SERIAL_LABEL("SERIAL"),
    FLAG_LABEL("BANDEIRA"),
    NSU_LABEL("NSU"),
    ERROR_LABEL("ERRO")
}