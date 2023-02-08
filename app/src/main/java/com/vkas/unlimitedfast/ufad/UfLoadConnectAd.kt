package com.vkas.unlimitedfast.ufad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufbean.UfAdBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getAdServerDataUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdClickUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdDisplaysUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.takeSortedAdIDUf
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JSONUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class UfLoadConnectAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadUf
    }

    object InstanceHelper {
        val openLoadUf = UfLoadConnectAd()
    }

    var appAdDataUf: InterstitialAd? = null

    // 是否正在加载中
    var isLoadingUf = false

    //加载时间
    private var loadTimeUf: Long = Date().time

    // 是否展示
    var whetherToShowUf = false

    // openIndex
    var adIndexUf = 0

    // 广告ID
    var idUf = ""

    /**
     * 广告加载前判断
     */
    fun advertisementLoadingUf(context: Context) {
        App.isAppOpenSameDayUf()
        if (UnLimitedUtils.isThresholdReached()) {
            KLog.d(logTagUf, "广告达到上线")
            return
        }
        KLog.d(logTagUf, "connect--isLoading=${isLoadingUf}")

        if (isLoadingUf) {
            KLog.d(logTagUf, "connect--广告加载中，不能再次加载")
            return
        }

        if (appAdDataUf == null) {
            isLoadingUf = true
            loadConnectAdvertisementUf(context, getAdServerDataUf())
        }
        if (appAdDataUf != null && !whetherAdExceedsOneHour(loadTimeUf)) {
            isLoadingUf = true
            appAdDataUf = null
            loadConnectAdvertisementUf(context, getAdServerDataUf())
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour
    }


    /**
     * 加载首页插屏广告
     */
    private fun loadConnectAdvertisementUf(context: Context, adData: UfAdBean) {
        val adRequest = AdRequest.Builder().build()
        idUf = takeSortedAdIDUf(adIndexUf, adData.uf_connect)
        KLog.d(
            logTagUf,
            "connect--插屏广告id=$idUf;权重=${adData.uf_connect.getOrNull(adIndexUf)?.uf_weight}"
        )

        InterstitialAd.load(
            context,
            idUf,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagUf, "connect---连接插屏加载失败=$it") }
                    isLoadingUf = false
                    appAdDataUf = null
                    if (adIndexUf < adData.uf_connect.size - 1) {
                        adIndexUf++
                        loadConnectAdvertisementUf(context, adData)
                    } else {
                        adIndexUf = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimeUf = Date().time
                    isLoadingUf = false
                    appAdDataUf = interstitialAd
                    adIndexUf = 0
                    KLog.d(logTagUf, "connect---连接插屏加载成功")
                }
            })
    }

    /**
     * connect插屏广告回调
     */
    private fun connectScreenAdCallback() {
        appAdDataUf?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagUf, "connect插屏广告点击")
                    recordNumberOfAdClickUf()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagUf, "关闭connect插屏广告=${App.isBackDataUf}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_UF_ADVERTISEMENT_SHOW)
                        .post(App.isBackDataUf)

                    appAdDataUf = null
                    whetherToShowUf = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(logTagUf, "Ad failed to show fullscreen content.")
                    appAdDataUf = null
                    whetherToShowUf = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdDataUf = null
                    recordNumberOfAdDisplaysUf()
                    // Called when ad is shown.
                    whetherToShowUf = true
                    KLog.d(logTagUf, "connect----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayConnectAdvertisementUf(activity: AppCompatActivity): Boolean {
        if (appAdDataUf == null) {
            KLog.d(logTagUf, "connect--插屏广告加载中。。。")
            return false
        }

        if (whetherToShowUf || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagUf, "connect--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        connectScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdDataUf as InterstitialAd).show(activity)
        }
        return true
    }
}