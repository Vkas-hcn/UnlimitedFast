package com.vkas.unlimitedfast.ufbean

data class UfVpnBean (
    var uf_city: String? = null,
    var uf_country: String? = null,
    var uf_ip: String? = null,
    var uf_method: String? = null,
    var uf_port: Int? = null,
    var uf_pwd: String? = null,
    var uf_check: Boolean? = false,
    var uf_best: Boolean? = false
)