package com.vkas.unlimitedfast.ufad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
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
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

class UfLoadOpenAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadUf
    }

    object InstanceHelper {
        val openLoadUf = UfLoadOpenAd()
    }

    var appAdDataUf: Any? = null

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
        KLog.d(logTagUf, "open--isLoading=${isLoadingUf}")

        if (isLoadingUf) {
            KLog.d(logTagUf, "open--广告加载中，不能再次加载")
            return
        }

        if (appAdDataUf == null) {
            isLoadingUf = true
            loadStartupPageAdvertisementUf(context, getAdServerDataUf())
        }
        if (appAdDataUf != null && !whetherAdExceedsOneHour(loadTimeUf)) {
            isLoadingUf = true
            appAdDataUf = null
            loadStartupPageAdvertisementUf(context, getAdServerDataUf())
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
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementUf(context: Context, adData: UfAdBean) {
        if (adData.uf_open.getOrNull(adIndexUf)?.uf_type == "screen") {
            loadStartInsertAdUf(context, adData)
        } else {
            loadOpenAdvertisementUf(context, adData)
        }
    }

    /**
     * 加载开屏广告
     */
    private fun loadOpenAdvertisementUf(context: Context, adData: UfAdBean) {
        KLog.e("loadOpenAdvertisementUf", "adData().uf_open=${JsonUtil.toJson(adData.uf_open)}")
        KLog.e(
            "loadOpenAdvertisementUf",
            "id=${JsonUtil.toJson(takeSortedAdIDUf(adIndexUf, adData.uf_open))}"
        )

        val id = takeSortedAdIDUf(adIndexUf, adData.uf_open)

        KLog.d(logTagUf, "open--开屏广告id=$id;权重=${adData.uf_open.getOrNull(adIndexUf)?.uf_weight}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadTimeUf = Date().time
                    isLoadingUf = false
                    appAdDataUf = ad

                    KLog.d(logTagUf, "open--开屏广告加载成功")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingUf = false
                    appAdDataUf = null
                    if (adIndexUf < adData.uf_open.size - 1) {
                        adIndexUf++
                        loadStartupPageAdvertisementUf(context, adData)
                    } else {
                        adIndexUf = 0
                    }
                    KLog.d(logTagUf, "open--开屏广告加载失败: " + loadAdError.message)
                }
            }
        )
    }


    /**
     * 开屏广告回调
     */
    private fun advertisingOpenCallbackUf() {
        if (appAdDataUf !is AppOpenAd) {
            return
        }
        (appAdDataUf as AppOpenAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                //取消全屏内容
                override fun onAdDismissedFullScreenContent() {
                    KLog.d(logTagUf, "open--关闭开屏内容")
                    whetherToShowUf = false
                    appAdDataUf = null
                    if (!App.whetherBackgroundUf) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                }

                //全屏内容无法显示时调用
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    whetherToShowUf = false
                    appAdDataUf = null
                    KLog.d(logTagUf, "open--全屏内容无法显示时调用")
                }

                //显示全屏内容时调用
                override fun onAdShowedFullScreenContent() {
                    appAdDataUf = null
                    whetherToShowUf = true
                    recordNumberOfAdDisplaysUf()
                    adIndexUf = 0
                    KLog.d(logTagUf, "open---开屏广告展示")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    KLog.d(logTagUf, "open---点击open广告")
                    recordNumberOfAdClickUf()
                }
            }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisementUf(activity: AppCompatActivity): Boolean {

        if (appAdDataUf == null) {
            KLog.d(logTagUf, "open---开屏广告加载中。。。")
            return false
        }
        if (whetherToShowUf || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagUf, "open---前一个开屏广告展示中或者生命周期不对")
            return false
        }
        if (appAdDataUf is AppOpenAd) {
            advertisingOpenCallbackUf()
            (appAdDataUf as AppOpenAd).show(activity)
        } else {
            startInsertScreenAdCallbackUf()
            (appAdDataUf as InterstitialAd).show(activity)
        }
        return true
    }

    /**
     * 加载启动页插屏广告
     */
    private fun loadStartInsertAdUf(context: Context, adData: UfAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDUf(adIndexUf, adData.uf_open)
        KLog.d(
            logTagUf,
            "open--插屏广告id=$id;权重=${adData.uf_open.getOrNull(adIndexUf)?.uf_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagUf, "open---连接插屏加载失败=$it") }
                    isLoadingUf = false
                    appAdDataUf = null
                    if (adIndexUf < adData.uf_open.size - 1) {
                        adIndexUf++
                        loadStartupPageAdvertisementUf(context, adData)
                    } else {
                        adIndexUf = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimeUf = Date().time
                    isLoadingUf = false
                    appAdDataUf = interstitialAd
                    KLog.d(logTagUf, "open--启动页插屏加载完成")
                }
            })
    }

    /**
     * StartInsert插屏广告回调
     */
    private fun startInsertScreenAdCallbackUf() {
        if (appAdDataUf !is InterstitialAd) {
            return
        }
        (appAdDataUf as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagUf, "open--插屏广告点击")
                    recordNumberOfAdClickUf()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagUf, "open--关闭StartInsert插屏广告${App.isBackDataUf}")
                    if (!App.whetherBackgroundUf) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
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
                    adIndexUf = 0
                    KLog.d(logTagUf, "open----插屏show")
                }
            }
    }
}