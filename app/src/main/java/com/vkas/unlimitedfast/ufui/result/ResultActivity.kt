package com.vkas.unlimitedfast.ufui.result

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.github.shadowsocks.Core
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.BR
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.databinding.ActivityResultUfBinding
import com.vkas.unlimitedfast.databinding.ActivityStartBinding
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufad.UfLoadResultAd
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufapp.App.Companion.mmkvUf
import com.vkas.unlimitedfast.ufbase.BaseActivity
import com.vkas.unlimitedfast.ufbase.BaseViewModel
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ResultActivity: BaseActivity<ActivityResultUfBinding, BaseViewModel>()  {
    private var isConnectionUf: Boolean = false

    //当前服务器
    private lateinit var currentServerBeanUf: UfVpnBean
    private var jobResultUf: Job? = null
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_result_uf
    }

    override fun initVariableId(): Int {
        return BR._all
    }
    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        isConnectionUf = bundle?.getBoolean(Constant.CONNECTION_UF_STATUS) == true
        currentServerBeanUf = JsonUtil.fromJson(
            bundle?.getString(Constant.SERVER_UF_INFORMATION),
            object : TypeToken<UfVpnBean?>() {}.type
        )
    }

    override fun initToolbar() {
        super.initToolbar()
        displayTimer()
        liveEventBusReceive()
        binding.presenter = UfClick()
        binding.resultTitle.tvTitle.text = if (isConnectionUf) {
            getString(R.string.vpn_connect)
        } else {
            getString(
                R.string.vpn_disconnect
            )
        }
        binding.resultTitle.tvRight.visibility = View.GONE
        binding.resultTitle.imgBack.setOnClickListener {
            finish()
        }
    }
fun liveEventBusReceive(){
    LiveEventBus
        .get(Constant.STOP_VPN_CONNECTION, Boolean::class.java)
        .observeForever {
            isConnectionUf = true
            binding.resultTitle.tvTitle.text = getString(R.string.vpn_disconnect)
            binding.tvConnected.text = getString(R.string.disconnection_succeed)
        }
}
    override fun initData() {
        super.initData()
        if (isConnectionUf) {
            binding.tvConnected.text = getString(R.string.connection_succeed)
        } else {
            binding.tvConnected.text = getString(R.string.disconnection_succeed)
            binding.txtTimerUf.text = mmkvUf.decodeString(Constant.LAST_TIME, "").toString()
        }
        binding.imgCountry.setImageResource(UnLimitedUtils.getFlagThroughCountryUf(currentServerBeanUf.uf_country.toString()))
        binding.txtCountry.text = currentServerBeanUf.uf_country.toString()
        UfLoadResultAd.getInstance().whetherToShowUf =false
        initResultAds()
    }

    inner class UfClick {

    }

    private fun initResultAds() {
        jobResultUf= lifecycleScope.launch {
            while (isActive) {
                UfLoadResultAd.getInstance().setDisplayResultNativeAd(this@ResultActivity,binding)
                if (UfLoadResultAd.getInstance().whetherToShowUf) {
                    jobResultUf?.cancel()
                    jobResultUf = null
                }
                delay(1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if(lifecycle.currentState != Lifecycle.State.RESUMED){return@launch}
            if(App.nativeAdRefreshUf){
                UfLoadResultAd.getInstance().whetherToShowUf =false
                if(UfLoadResultAd.getInstance().appAdDataUf !=null){
                    UfLoadResultAd.getInstance().setDisplayResultNativeAd(this@ResultActivity,binding)
                }else{
                    UfLoadResultAd.getInstance().advertisementLoadingUf(this@ResultActivity)
                    initResultAds()
                }
            }
        }
    }
    /**
     * 显示计时器
     */
    private fun displayTimer() {
        LiveEventBus
            .get(Constant.TIMER_UF_DATA, String::class.java)
            .observeForever {
                if (isConnectionUf) {
                    binding.txtTimerUf.text = it
                } else {
                    binding.txtTimerUf.text = mmkvUf.decodeString(Constant.LAST_TIME, "").toString()
                }
            }
    }
}