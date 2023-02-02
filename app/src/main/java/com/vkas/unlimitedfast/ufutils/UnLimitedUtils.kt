package com.vkas.unlimitedfast.ufutils

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.vkas.unlimitedfast.BuildConfig
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufapp.App.Companion.mmkvUf
import com.vkas.unlimitedfast.ufbean.UfAdBean
import com.vkas.unlimitedfast.ufbean.UfDetailBean
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResUtils.getString
import com.xuexiang.xutil.resource.ResourceUtils
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object UnLimitedUtils {
    /**
     * 获取Fast ip
     */
    fun getFastIpUf(): UfVpnBean {
        val ufVpnBean: MutableList<UfVpnBean> = getLocalServerData()
        var intersectionList = findFastAndOrdinaryIntersection(ufVpnBean)
        if (intersectionList.size <= 0) {
            intersectionList = ufVpnBean
        }
        intersectionList.shuffled().take(1).forEach {
            it.uf_best = true
            it.uf_country = getString(R.string.fast_service)
            return it
        }
        intersectionList[0].uf_best = true
        return intersectionList[0]
    }

    /**
     * 获取本地服务器数据
     */
    fun getLocalServerData(): MutableList<UfVpnBean> {
        return if (Utils.isNullOrEmpty(mmkvUf.decodeString(Constant.PROFILE_UF_DATA))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert(Constant.VPN_LOCAL_FILE_NAME_UF),
                object : TypeToken<MutableList<UfVpnBean>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvUf.decodeString(Constant.PROFILE_UF_DATA),
                object : TypeToken<MutableList<UfVpnBean>?>() {}.type
            )
        }
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        return if (Utils.isNullOrEmpty(mmkvUf.decodeString(Constant.PROFILE_UF_DATA_FAST))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert(Constant.FAST_LOCAL_FILE_NAME_UF),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvUf.decodeString(Constant.PROFILE_UF_DATA_FAST),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        }
    }
    /**
     *
     */

    /**
     * 找出fast与普通交集
     */
    private fun findFastAndOrdinaryIntersection(ufVpnBeans: MutableList<UfVpnBean>): MutableList<UfVpnBean> {
        val intersectionList: MutableList<UfVpnBean> = ArrayList()
        getLocalFastServerData().forEach { fast ->
            ufVpnBeans.forEach { skServiceBean ->
                if (fast == skServiceBean.uf_ip) {
                    intersectionList.add(skServiceBean)
                }
            }
        }
        return intersectionList
    }

    /**
     * 广告排序
     */
    private fun adSortingUf(elAdBean: UfAdBean): UfAdBean {
        val adBean: UfAdBean = UfAdBean()
        val elOpen = elAdBean.uf_open.sortedWith(compareByDescending { it.uf_weight })
        val elBack = elAdBean.uf_back.sortedWith(compareByDescending { it.uf_weight })

        val ufVpn = elAdBean.uf_vpn.sortedWith(compareByDescending { it.uf_weight })
        val elResult = elAdBean.uf_result.sortedWith(compareByDescending { it.uf_weight })
        val elConnect = elAdBean.uf_connect.sortedWith(compareByDescending { it.uf_weight })


        adBean.uf_open = elOpen.toMutableList()
        adBean.uf_back = elBack.toMutableList()

        adBean.uf_vpn = ufVpn.toMutableList()
        adBean.uf_result = elResult.toMutableList()
        adBean.uf_connect = elConnect.toMutableList()

        adBean.uf_show_num = elAdBean.uf_show_num
        adBean.uf_click_num = elAdBean.uf_click_num
        return adBean
    }

    /**
     * 取出排序后的广告ID
     */
    fun takeSortedAdIDUf(index: Int, elAdDetails: MutableList<UfDetailBean>): String {
        return elAdDetails.getOrNull(index)?.uf_id ?: ""
    }

    /**
     * 获取广告服务器数据
     */
    fun getAdServerDataUf(): UfAdBean {
        val serviceData: UfAdBean =
            if (Utils.isNullOrEmpty(mmkvUf.decodeString(Constant.ADVERTISING_UF_DATA))) {
                JsonUtil.fromJson(
                    ResourceUtils.readStringFromAssert(Constant.AD_LOCAL_FILE_NAME_UF),
                    object : TypeToken<
                            UfAdBean?>() {}.type
                )
            } else {
                JsonUtil.fromJson(
                    mmkvUf.decodeString(Constant.ADVERTISING_UF_DATA),
                    object : TypeToken<UfAdBean?>() {}.type
                )
            }
        return adSortingUf(serviceData)
    }

    /**
     * 是否达到阀值
     */
    fun isThresholdReached(): Boolean {
        val clicksCount = mmkvUf.decodeInt(Constant.CLICKS_UF_COUNT, 0)
        val showCount = mmkvUf.decodeInt(Constant.SHOW_UF_COUNT, 0)
        KLog.e("TAG", "clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e(
            "TAG",
            "uf_click_num=${getAdServerDataUf().uf_click_num}, getAdServerData().uf_show_num=${getAdServerDataUf().uf_show_num}"
        )
        if (clicksCount >= getAdServerDataUf().uf_click_num || showCount >= getAdServerDataUf().uf_show_num) {
            return true
        }
        return false
    }

    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplaysUf() {
        var showCount = mmkvUf.decodeInt(Constant.SHOW_UF_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_UF_COUNT, showCount)
    }

    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClickUf() {
        var clicksCount = mmkvUf.decodeInt(Constant.CLICKS_UF_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_UF_COUNT, clicksCount)
    }

    /**
     * 通过国家获取国旗
     */
    fun getFlagThroughCountryUf(uf_country: String): Int {
        when (uf_country) {
            "Faster server" -> {
                return R.mipmap.ic_fast
            }
            "Japan" -> {
                return R.mipmap.ic_japan
            }
            "United Kingdom" -> {
                return R.mipmap.ic_unitedkingdom
            }
            "United States" -> {
                return R.mipmap.ic_unitedstates
            }
            "Australia" -> {
                return R.mipmap.ic_australia
            }
            "Belgium" -> {
                return R.mipmap.ic_belgium
            }
            "Brazil" -> {
                return R.mipmap.ic_brazil
            }
            "Canada" -> {
                return R.mipmap.ic_canada
            }
            "France" -> {
                return R.mipmap.ic_france
            }
            "Germany" -> {
                return R.mipmap.ic_germany
            }
            "India" -> {
                return R.mipmap.ic_india
            }
            "Ireland" -> {
                return R.mipmap.ic_ireland
            }
            "Italy" -> {
                return R.mipmap.ic_italy
            }
            "SouthKorea" -> {
                return R.mipmap.ic_koreasouth
            }
            "Netherlands" -> {
                return R.mipmap.ic_netherlands
            }
            "Newzealand" -> {
                return R.mipmap.ic_newzealand
            }
            "Norway" -> {
                return R.mipmap.ic_norway
            }
            "Russianfederation" -> {
                return R.mipmap.ic_russianfederation
            }
            "Singapore" -> {
                return R.mipmap.ic_singapore
            }
            "Sweden" -> {
                return R.mipmap.ic_sweden
            }
            "Switzerland" -> {
                return R.mipmap.ic_switzerland
            }
        }

        return R.mipmap.ic_fast
    }

    fun getIpInformation() {
        val sb = StringBuffer()
        try {
            val url = URL("https://ip.seeip.org/geoip/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val `is` = conn.inputStream
                val b = ByteArray(1024)
                var len: Int
                while (`is`.read(b).also { len = it } != -1) {
                    sb.append(String(b, 0, len, Charset.forName("UTF-8")))
                }
                `is`.close()
                conn.disconnect()
                KLog.e("state", "sb==${sb.toString()}")
                MmkvUtils.set(Constant.IP_INFORMATION, sb.toString())
            } else {
                MmkvUtils.set(Constant.IP_INFORMATION, "")
                KLog.e("state", "code==${code.toString()}")
            }
        } catch (var1: Exception) {
            MmkvUtils.set(Constant.IP_INFORMATION, "")
            KLog.e("state", "Exception==${var1.message}")
        }
    }

    /**
     * 埋点
     */
    fun getBuriedPointUf(name: String) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.logEvent(name, null)
        } else {
            KLog.d(logTagUf, "触发埋点----name=${name}")
        }
    }

    /**
     * 埋点
     */
    fun getBuriedPointUserTypeUf(name: String, value: String) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.setUserProperty(name, value)
        } else {
            KLog.d(logTagUf, "触发埋点----name=${name}-----value=${value}")
        }
    }
    /**
     * 埋点连接时长
     */
    fun getBuriedPointConnectionTimeUf(name: String,time:Int) {
        if (!BuildConfig.DEBUG) {
            Firebase.analytics.logEvent(name, bundleOf("time" to time))
        } else {
            KLog.d(logTagUf, "触发埋点----name=${name}---time=${time}")
        }
    }
}