package com.vkas.unlimitedfast.ufad

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.vkas.unlimitedfast.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.unlimitedfast.databinding.ActivityResultUfBinding
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufbean.UfAdBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getAdServerDataUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdClickUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdDisplaysUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.takeSortedAdIDUf
import java.util.*

class UfLoadResultAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadUf
    }

    object InstanceHelper {
        val openLoadUf = UfLoadResultAd()
    }

    var appAdDataUf: NativeAd? = null

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
        KLog.d(logTagUf, "result--isLoading=${isLoadingUf}")

        if (isLoadingUf) {
            KLog.d(logTagUf, "result--广告加载中，不能再次加载")
            return
        }
        if (appAdDataUf == null) {
            isLoadingUf = true
            loadResultAdvertisementUf(context, getAdServerDataUf())
        }
        if (appAdDataUf != null && !whetherAdExceedsOneHour(loadTimeUf)) {
            isLoadingUf = true
            appAdDataUf = null
            loadResultAdvertisementUf(context, getAdServerDataUf())
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
     * 加载result原生广告
     */
    private fun loadResultAdvertisementUf(context: Context, adData: UfAdBean) {
        val id = takeSortedAdIDUf(adIndexUf, adData.uf_result)
        KLog.d(
            logTagUf,
            "result---原生广告id=$id;权重=${adData.uf_result.getOrNull(adIndexUf)?.uf_weight}"
        )

        val homeNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOelions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        homeNativeAds.withNativeAdOptions(adOelions)
        homeNativeAds.forNativeAd {
            appAdDataUf = it
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoadingUf = false
                appAdDataUf = null
                KLog.d(logTagUf, "result---加载result原生加载失败: $error")

                if (adIndexUf < adData.uf_result.size - 1) {
                    adIndexUf++
                    loadResultAdvertisementUf(context, adData)
                } else {
                    adIndexUf = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagUf, "result---加载result原生广告成功")
                loadTimeUf = Date().time
                isLoadingUf = false
                adIndexUf = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagUf, "result---点击result原生广告")
                recordNumberOfAdClickUf()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示home原生广告
     */
    fun setDisplayResultNativeAd(activity: AppCompatActivity, binding: ActivityResultUfBinding) {
        activity.runOnUiThread {
            appAdDataUf.let {
                if (it != null && !whetherToShowUf && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        it.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_result_native_uf, null) as NativeAdView
                    // 对应原生组件
                    setResultNativeComponent(it, adView)
                    binding.ufAdFrame.removeAllViews()
                    binding.ufAdFrame.addView(adView)
                    binding.resultAdUf = true
                    recordNumberOfAdDisplaysUf()
                    whetherToShowUf = true
                    App.nativeAdRefreshUf = false
                    appAdDataUf = null
                    KLog.d(logTagUf, "result--原生广告--展示")
                    //重新缓存
                    advertisementLoadingUf(activity)
                }
            }

        }
    }

    private fun setResultNativeComponent(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as TextView).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}