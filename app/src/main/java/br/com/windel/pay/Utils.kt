package br.com.windel.pay

class Utils {
    fun convertToIntVero(value: String): Int {
        return value.replace(",", "").toInt()
    }
}