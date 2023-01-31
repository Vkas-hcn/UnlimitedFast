package com.vkas.unlimitedfast.ufapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Job
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.blankj.utilcode.util.ProcessUtils
import com.github.shadowsocks.Core
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.BuildConfig
import com.vkas.unlimitedfast.ufui.main.MainActivity
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufbase.AppManagerUfMVVM
import com.vkas.unlimitedfast.ufui.start.StartActivity
import com.vkas.unlimitedfast.ufutils.ActivityUtils
import com.vkas.unlimitedfast.ufutils.CalendarUtils
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.MmkvUtils
import com.vkas.unlimitedfast.ufutils.UfTimerThread.sendTimerInformation

class App : Application(), LifecycleObserver {
    private var flag = 0
    private var job_uf : Job? =null
    private var ad_activity_uf: Activity? = null
    private var top_activity_uf: Activity? = null
    companion object {
        // app当前是否在后台
        var isBackDataUf = false

        // 是否进入后台（三秒后）
        var whetherBackgroundUf = false
        // 原生广告刷新
        var nativeAdRefreshUf = false
        val mmkvUf by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("UnlimitedFast", MMKV.MULTI_PROCESS_MODE)
        }
        //当日日期
        var adDateUf = ""
        /**
         * 判断是否是当天打开
         */
        fun isAppOpenSameDayUf() {
            adDateUf = mmkvUf.decodeString(Constant.CURRENT_UF_DATE, "").toString()
            if (adDateUf == "") {
                MmkvUtils.set(Constant.CURRENT_UF_DATE, CalendarUtils.formatDateNow())
            } else {
                if (CalendarUtils.dateAfterDate(adDateUf, CalendarUtils.formatDateNow())) {
                    MmkvUtils.set(Constant.CURRENT_UF_DATE, CalendarUtils.formatDateNow())
                    MmkvUtils.set(Constant.CLICKS_UF_COUNT, 0)
                    MmkvUtils.set(Constant.SHOW_UF_COUNT, 0)
                }
            }
        }

    }
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
//        initCrash()
        setActivityLifecycleUf(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
            XUI.init(this) //初始化UI框架
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)
        sendTimerInformation()
        isAppOpenSameDayUf()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        nativeAdRefreshUf =true
        job_uf?.cancel()
        job_uf = null
        //从后台切过来，跳转启动页
        if (whetherBackgroundUf&& !isBackDataUf) {
            jumpGuidePage()
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState(){
        job_uf = GlobalScope.launch {
            whetherBackgroundUf = false
            delay(3000L)
            whetherBackgroundUf = true
            ad_activity_uf?.finish()
            ActivityUtils.getActivity(StartActivity::class.java)?.finish()
        }
    }
    /**
     * 跳转引导页
     */
    private fun jumpGuidePage(){
        whetherBackgroundUf = false
        val intent = Intent(top_activity_uf, StartActivity::class.java)
        intent.putExtra(Constant.RETURN_UF_CURRENT_PAGE, true)
        top_activity_uf?.startActivity(intent)
    }
    fun setActivityLifecycleUf(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppManagerUfMVVM.get().addActivity(activity)
                if (activity !is AdActivity) {
                    top_activity_uf = activity
                } else {
                    ad_activity_uf = activity
                }
                KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
            }

            override fun onActivityStarted(activity: Activity) {
                KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_uf = activity
                } else {
                    ad_activity_uf = activity
                }
                flag++
                isBackDataUf = false
            }

            override fun onActivityResumed(activity: Activity) {
                KLog.v("Lifecycle", "onActivityResumed=" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_uf = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is AdActivity) {
                    ad_activity_uf = activity
                } else {
                    top_activity_uf = activity
                }
                KLog.v("Lifecycle", "onActivityPaused=" + activity.javaClass.name)
            }

            override fun onActivityStopped(activity: Activity) {
                flag--
                if (flag == 0) {
                    isBackDataUf = true
                }
                KLog.v("Lifecycle", "onActivityStopped=" + activity.javaClass.name)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                KLog.v("Lifecycle", "onActivitySaveInstanceState=" + activity.javaClass.name)

            }

            override fun onActivityDestroyed(activity: Activity) {
                AppManagerUfMVVM.get().removeActivity(activity)
                KLog.v("Lifecycle", "onActivityDestroyed" + activity.javaClass.name)
                ad_activity_uf = null
                top_activity_uf = null
            }
        })
    }
}