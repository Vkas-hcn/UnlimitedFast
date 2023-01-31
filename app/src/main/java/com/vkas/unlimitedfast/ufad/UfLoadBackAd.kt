package com.vkas.unlimitedfast.ufad

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufbean.UfAdBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.RoundCornerOutlineProvider
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getAdServerDataUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdClickUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdDisplaysUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.takeSortedAdIDUf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class UfLoadBackAd {
    companion object {
        fun getInstance() = InstanceHelper.backLoadUf
    }

    object InstanceHelper {
        val backLoadUf = UfLoadBackAd()
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

    /**
     * 广告加载前判断
     */
    fun advertisementLoadingUf(context: Context) {
        App.isAppOpenSameDayUf()
        if (UnLimitedUtils.isThresholdReached()) {
            KLog.d(logTagUf, "广告达到上线")
            return
        }
        KLog.d(logTagUf, "back--isLoading=${isLoadingUf}")

        if (isLoadingUf) {
            KLog.d(logTagUf, "back--广告加载中，不能再次加载")
            return
        }

        if(appAdDataUf == null){
            isLoadingUf = true
            loadBackAdvertisementUf(context,getAdServerDataUf())
        }
        if (appAdDataUf != null && !whetherAdExceedsOneHour(loadTimeUf)) {
            isLoadingUf = true
            appAdDataUf =null
            loadBackAdvertisementUf(context,getAdServerDataUf())
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
    private fun loadBackAdvertisementUf(context: Context, adData: UfAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDUf(adIndexUf, adData.uf_back)
        KLog.d(logTagUf, "back--插屏广告id=$id;权重=${adData.uf_back.getOrNull(adIndexUf)?.uf_weight}")

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let {
                        KLog.d(logTagUf, "back---连接插屏加载失败=$it") }
                    isLoadingUf = false
                    appAdDataUf = null
                    if (adIndexUf < adData.uf_back.size - 1) {
                        adIndexUf++
                        loadBackAdvertisementUf(context,adData)
                    }else{
                        adIndexUf = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimeUf = Date().time
                    isLoadingUf = false
                    appAdDataUf = interstitialAd
                    adIndexUf = 0
                    KLog.d(logTagUf, "back---返回插屏加载成功")
                }
            })
    }

    /**
     * back插屏广告回调
     */
    private fun backScreenAdCallback() {
        appAdDataUf?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagUf, "back插屏广告点击")
                    recordNumberOfAdClickUf()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagUf, "关闭back插屏广告${App.isBackDataUf}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_UF_BACK_AD_SHOW)
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
                    KLog.d(logTagUf, "back----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayBackAdvertisementUf(activity: AppCompatActivity): Boolean {
        if (appAdDataUf == null) {
            KLog.d(logTagUf, "back--插屏广告加载中。。。")
            return false
        }
        if (whetherToShowUf || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagUf, "back--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        backScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdDataUf as InterstitialAd).show(activity)
        }
        return true
    }
}