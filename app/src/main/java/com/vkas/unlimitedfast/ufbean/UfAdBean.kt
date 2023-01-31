package com.vkas.unlimitedfast.ufbean

import androidx.annotation.Keep

data class UfAdBean(
    var uf_open: MutableList<UfDetailBean> = ArrayList(),
    var uf_back: MutableList<UfDetailBean> = ArrayList(),
    var uf_vpn: MutableList<UfDetailBean> = ArrayList(),
    var uf_result: MutableList<UfDetailBean> = ArrayList(),
    var uf_connect: MutableList<UfDetailBean> = ArrayList(),

    var uf_click_num: Int = 0,
    var uf_show_num: Int = 0
)

@Keep
data class UfDetailBean(
    val uf_id: String,
    val uf_platform: String,
    val uf_type: String,
    val uf_weight: Int
)