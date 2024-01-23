package br.com.windel.pos.enums

import br.com.windel.pos.BuildConfig

enum class EndpointEnum(val value: String){
    GATEWAY_VERO_ORDER("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/order"),
    GATEWAY_VERO_TERMINAL("${BuildConfig.WINDEL_POS_HOST}/gateway-vero/terminal")
}