package br.com.windel.pay.enums

enum class EventsEnum(val value: String){
    EVENT_PAY("pay"),
    EVENT_PROCESSING("processing"),
    EVENT_SUCCESS("success"),
    EVENT_FAILED("failed"),
    EVENT_CANCELED("canceled")
}