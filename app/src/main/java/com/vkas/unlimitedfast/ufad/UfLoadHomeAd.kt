package com.vkas.unlimitedfast.ufad

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.unlimitedfast.databinding.ActivityMainBinding
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufbean.UfAdBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getAdServerDataUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdClickUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.takeSortedAdIDUf
import java.util.*
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.ufutils.RoundCornerOutlineProvider
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.recordNumberOfAdDisplaysUf

class UfLoadHomeAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadUf
    }

    object InstanceHelper {
        val openLoadUf = UfLoadHomeAd()
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
        KLog.d(logTagUf, "home--isLoading=${isLoadingUf}")

        if (isLoadingUf) {
            KLog.d(logTagUf, "home--广告加载中，不能再次加载")
            return
        }
        if(appAdDataUf == null){
            isLoadingUf = true
            loadHomeAdvertisementUf(context,getAdServerDataUf())
        }
        if (appAdDataUf != null && !whetherAdExceedsOneHour(loadTimeUf)) {
            isLoadingUf = true
            appAdDataUf =null
            loadHomeAdvertisementUf(context,getAdServerDataUf())
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
     * 加载vpn原生广告
     */
    private fun loadHomeAdvertisementUf(context: Context,adData: UfAdBean) {
        val id = takeSortedAdIDUf(adIndexUf, adData.uf_vpn)
        KLog.d(logTagUf, "home---原生广告id=$id;权重=${adData.uf_vpn.getOrNull(adIndexUf)?.uf_weight}")

        val vpnNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        vpnNativeAds.withNativeAdOptions(adOptions)
        vpnNativeAds.forNativeAd {
            appAdDataUf = it
        }
        vpnNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoadingUf = false
                appAdDataUf = null
                KLog.d(logTagUf, "home---加载vpn原生加载失败: $error")

                if (adIndexUf < adData.uf_vpn.size - 1) {
                    adIndexUf++
                    loadHomeAdvertisementUf(context,adData)
                }else{
                    adIndexUf = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagUf, "home---加载vpn原生广告成功")
                loadTimeUf = Date().time
                isLoadingUf = false
                adIndexUf = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagUf, "home---点击vpn原生广告")
                recordNumberOfAdClickUf()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示vpn原生广告
     */
    fun setDisplayHomeNativeAdUf(activity: AppCompatActivity, binding: ActivityMainBinding) {
        activity.runOnUiThread {
            appAdDataUf.let {
                if (it != null && !whetherToShowUf&& activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        it.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_main_native_uf, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponentUf(it, adView)
                    binding.ufAdFrame.removeAllViews()
                    binding.ufAdFrame.addView(adView)
                    binding.vpnAdUf = true
                    recordNumberOfAdDisplaysUf()
                    whetherToShowUf = true
                    App.nativeAdRefreshUf = false
                    appAdDataUf = null
                    KLog.d(logTagUf, "home--原生广告--展示")
                    //重新缓存
                    advertisementLoadingUf(activity)
                }
            }

        }
    }

    private fun setCorrespondingNativeComponentUf(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        adView.mediaView.clipToOutline=true
        adView.mediaView.outlineProvider= RoundCornerOutlineProvider(8f)

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