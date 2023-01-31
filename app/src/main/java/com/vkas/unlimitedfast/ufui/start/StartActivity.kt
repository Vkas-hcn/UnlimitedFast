package com.vkas.unlimitedfast.ufui.start

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.BR
import com.vkas.unlimitedfast.BuildConfig
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.databinding.ActivityStartBinding
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufad.*
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufbase.BaseActivity
import com.vkas.unlimitedfast.ufbase.BaseViewModel
import com.vkas.unlimitedfast.ufui.main.MainActivity
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.MmkvUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.isThresholdReached
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import kotlinx.coroutines.*

class StartActivity: BaseActivity<ActivityStartBinding, BaseViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener  {
    companion object {
        var isCurrentPage: Boolean = false
    }
    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsUf: Job? = null

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_start
    }

    override fun initVariableId(): Int {
        return BR._all
    }
    override fun initParam() {
        super.initParam()
        isCurrentPage = intent.getBooleanExtra(Constant.RETURN_UF_CURRENT_PAGE, false)

    }
    override fun initToolbar() {
        super.initToolbar()
    }

    override fun initData() {
        super.initData()
        binding.pbStartUf.setProgressViewUpdateListener(this)
        binding.pbStartUf.setProgressDuration(8000)
        binding.pbStartUf.startProgressAnimation()
        liveEventBusUf()
        lifecycleScope.launch(Dispatchers.IO) {
            UnLimitedUtils.getIpInformation()
        }
        getFirebaseDataUf()
        jumpHomePageData()
    }
    private fun liveEventBusUf() {
        LiveEventBus
            .get(Constant.OPEN_CLOSE_JUMP, Boolean::class.java)
            .observeForever {
                KLog.d(logTagUf, "关闭开屏内容-接收==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    private fun getFirebaseDataUf() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            lifecycleScope.launch {
//                delay(1500)
//                MmkvUtils.set(Constant.ADVERTISING_UF_DATA, ResourceUtils.readStringFromAssert("elAdDataFireBase.json"))
//            }
            return
        } else {
            preloadedAdvertisement()
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Constant.PROFILE_UF_DATA, auth.getString("easy_servers"))
                MmkvUtils.set(Constant.PROFILE_UF_DATA_FAST, auth.getString("easy_smart"))
                MmkvUtils.set(Constant.AROUND_UF_FLOW_DATA, auth.getString("UfAroundFlow_Data"))
                MmkvUtils.set(Constant.ADVERTISING_UF_DATA, auth.getString("easy_ad"))

            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun jumpHomePageData() {
        liveJumpHomePage2.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                KLog.e("TAG", "isBackDataUf==${App.isBackDataUf}")
                delay(300)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    jumpPage()
                }
            }
        })
        liveJumpHomePage.observe(this, {
            liveJumpHomePage2.postValue(true)
        })
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (!isCurrentPage) {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()

    }
    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        // 开屏
        UfLoadOpenAd.getInstance().adIndexUf = 0
        UfLoadOpenAd.getInstance().advertisementLoadingUf(this)
        rotationDisplayOpeningAdUf()
        // 首页原生
        UfLoadHomeAd.getInstance().adIndexUf = 0
        UfLoadHomeAd.getInstance().advertisementLoadingUf(this)
        // 结果页原生
        UfLoadResultAd.getInstance().adIndexUf = 0
        UfLoadResultAd.getInstance().advertisementLoadingUf(this)
        // 连接插屏
        UfLoadConnectAd.getInstance().adIndexUf = 0
        UfLoadConnectAd.getInstance().advertisementLoadingUf(this)
        // 服务器页插屏
        UfLoadBackAd.getInstance().adIndexUf = 0
        UfLoadBackAd.getInstance().advertisementLoadingUf(this)
    }
    /**
     * 轮训展示开屏广告
     */
    private fun rotationDisplayOpeningAdUf() {
        jobOpenAdsUf = lifecycleScope.launch {
            try {
                withTimeout(8000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState = UfLoadOpenAd.getInstance()
                            .displayOpenAdvertisementUf(this@StartActivity)
                        if (showState) {
                            jobOpenAdsUf?.cancel()
                            jobOpenAdsUf =null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }
    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
        App.isAppOpenSameDayUf()
        if (isThresholdReached()) {
            KLog.d(logTagUf, "广告达到上线")
            lifecycleScope.launch {
                delay(2000L)
                liveJumpHomePage.postValue(true)
            }
        } else {
            loadAdvertisement()
        }
    }
    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }
}