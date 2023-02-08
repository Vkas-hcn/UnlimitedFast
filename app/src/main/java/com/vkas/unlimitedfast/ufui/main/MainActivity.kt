package com.vkas.unlimitedfast.ufui.main

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.BR
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.databinding.ActivityMainBinding
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.enevt.Constant.logTagUf
import com.vkas.unlimitedfast.ufad.UfLoadBackAd
import com.vkas.unlimitedfast.ufad.UfLoadConnectAd
import com.vkas.unlimitedfast.ufad.UfLoadHomeAd
import com.vkas.unlimitedfast.ufad.UfLoadResultAd
import com.vkas.unlimitedfast.ufapp.App
import com.vkas.unlimitedfast.ufapp.App.Companion.mmkvUf
import com.vkas.unlimitedfast.ufbase.BaseActivity
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.vkas.unlimitedfast.ufui.list.VpnListActivity
import com.vkas.unlimitedfast.ufui.result.ResultActivity
import com.vkas.unlimitedfast.ufui.webuf.WebUfActivity
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.MmkvUtils
import com.vkas.unlimitedfast.ufutils.UfTimerThread
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getFlagThroughCountryUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.isThresholdReached
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    var state = BaseService.State.Idle

    //重复点击
    var repeatClick = false
    private var jobRepeatClick: Job? = null

    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    private val connection = ShadowsocksConnection(true)

    // 是否返回刷新服务器
    var whetherRefreshServer = false
    private var jobNativeAdsUf: Job? = null
    private var jobStartUf: Job? = null

    //当前执行连接操作
    private var performConnectionOperations: Boolean = false

    //是否点击连接
    private var clickToConnect: Boolean = false

    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = UfClick()
        liveEventBusReceive()
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Constant.TIMER_UF_DATA, String::class.java)
            .observeForever {
                binding.txtTimerUf.text = it
            }
        LiveEventBus
            .get(Constant.STOP_VPN_CONNECTION, Boolean::class.java)
            .observeForever {
                if (state.canStop) {
                    performConnectionOperations = false
                    Core.stopService()
                }
            }

        //更新服务器(未连接)
        LiveEventBus
            .get(Constant.NOT_CONNECTED_UF_RETURN, UfVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, false)
            }
        //更新服务器(已连接)
        LiveEventBus
            .get(Constant.CONNECTED_UF_RETURN, UfVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, true)
            }
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_UF_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observeForever {
                KLog.e("state", "插屏关闭接收=${it}")

                //重复点击
                jobRepeatClick = lifecycleScope.launch {
                    if (!repeatClick) {
                        KLog.e("state", "插屏关闭后跳转=${it}")
                        UfLoadConnectAd.getInstance().advertisementLoadingUf(this@MainActivity)
                        connectOrDisconnectUf(it)
                        repeatClick = true
                    }
                    delay(1000)
                    repeatClick = false
                }
            }
    }

    override fun initData() {
        super.initData()
        if (viewModel.whetherParsingIsIllegalIp()) {
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        if (UfTimerThread.isStopThread) {
            viewModel.initializeServerData()
        } else {
            val serviceData = mmkvUf.decodeString("currentServerData", "").toString()
            val currentServerData: UfVpnBean = JsonUtil.fromJson(
                serviceData,
                object : TypeToken<UfVpnBean?>() {}.type
            )
            setFastInformation(currentServerData)
        }
        UfLoadHomeAd.getInstance().whetherToShowUf = false
        initHomeAd()
    }

    private fun initHomeAd() {
        jobNativeAdsUf = lifecycleScope.launch {
            while (isActive) {
                UfLoadHomeAd.getInstance().setDisplayHomeNativeAdUf(this@MainActivity, binding)
                if (UfLoadHomeAd.getInstance().whetherToShowUf) {
                    jobNativeAdsUf?.cancel()
                    jobNativeAdsUf = null
                }
                delay(1000L)
            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
        // 跳转结果页
        jumpResultsPageData()
        setServiceData()
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    startActivityForResult(ResultActivity::class.java, 0x11, it)
                }
            }
        })
        viewModel.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)
        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            connect.launch(null)
        })
    }

    inner class UfClick {
        fun linkService() {
            if (binding.vpnState != 1) {
                connect.launch(null)
            }
            if (binding.vpnState == 0) {
                UnLimitedUtils.getBuriedPointUf("unlimF_clickv")
            }
        }

        fun clickService() {
            if (binding.vpnState != 1) {
                jumpToServerList()
            }
        }

        fun openOrCloseMenu() {
            binding.sidebarShowsUf = binding.sidebarShowsUf != true
        }

        fun clickMain() {
            KLog.e("TAG", "binding.sidebarShowsUf===>${binding.sidebarShowsUf}")
            if (binding.sidebarShowsUf == true) {
                binding.sidebarShowsUf = false
            }
        }

        fun clickMainMenu() {

        }

        fun toContactUs() {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_UF_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            runCatching {
                startActivity(intent)
            }.onFailure {
                ToastUtils.toast("Please set up a Mail account")
            }
        }

        fun toPrivacyPolicy() {
            startActivity(WebUfActivity::class.java)
        }

        fun toShare() {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(
                Intent.EXTRA_TEXT,
                Constant.SHARE_UF_ADDRESS + this@MainActivity.packageName
            )
            intent.type = "text/plain"
            startActivity(intent)
        }
    }

    /**
     * 跳转服务器列表
     */
    fun jumpToServerList() {
        lifecycleScope.launch {
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            val bundle = Bundle()
            if (state.name == "Connected") {
                bundle.putBoolean(Constant.WHETHER_UF_CONNECTED, true)
            } else {
                bundle.putBoolean(Constant.WHETHER_UF_CONNECTED, false)
            }
            UfLoadBackAd.getInstance().advertisementLoadingUf(this@MainActivity)
            val serviceData = mmkvUf.decodeString("currentServerData", "").toString()
            bundle.putString(Constant.CURRENT_UF_SERVICE, serviceData)
            startActivity(VpnListActivity::class.java, bundle)
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(elVpnBean: UfVpnBean) {
        if (elVpnBean.uf_best == true) {
            binding.txtCountry.text = Constant.FASTER_UF_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountryUf(Constant.FASTER_UF_SERVER))
        } else {
            binding.txtCountry.text = elVpnBean.uf_country.toString()
            binding.imgCountry.setImageResource(getFlagThroughCountryUf(elVpnBean.uf_country.toString()))
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        lifecycleScope.launch(Dispatchers.IO) {
            UnLimitedUtils.getIpInformation()
        }
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
            UnLimitedUtils.getBuriedPointUf("unlimF_geta")
            if (isNetworkAvailable()) {
                startVpn()
            } else {
                ToastUtils.toast("Please check your network")
            }
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        binding.vpnState = 1
        clickToConnect =true
        changeOfVpnStatus()
        jobStartUf = lifecycleScope.launch {
            App.isAppOpenSameDayUf()
            if (isThresholdReached() || Utils.isNullOrEmpty(UfLoadConnectAd.getInstance().idUf)) {
                KLog.d(logTagUf, "广告达到上线,或者无广告位")
                delay(1500)
                connectOrDisconnectUf(false)
                return@launch
            }
            UfLoadConnectAd.getInstance().advertisementLoadingUf(this@MainActivity)
            UfLoadResultAd.getInstance().advertisementLoadingUf(this@MainActivity)

            try {
                withTimeout(10000L) {
                    delay(1500L)
                    KLog.e(logTagUf, "jobStartUf?.isActive=${jobStartUf?.isActive}")
                    while (jobStartUf?.isActive == true) {
                        val showState =
                            UfLoadConnectAd.getInstance()
                                .displayConnectAdvertisementUf(this@MainActivity)
                        if (showState) {
                            jobStartUf?.cancel()
                            jobStartUf = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.d(logTagUf, "connect---插屏超时")
                if (jobStartUf != null) {
                    connectOrDisconnectUf(false)
                }
            }
        }
    }

    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnectUf(isBackgroundClosed: Boolean) {
        KLog.e("state", "连接或断开")
        if (viewModel.whetherParsingIsIllegalIp()) {
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }
        performConnectionOperations = if (state.canStop) {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(false)
            }
            Core.stopService()
            false
        } else {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(true)
            }
            UnLimitedUtils.getBuriedPointUf("unlimF_sv")
            Core.startService()
            true
        }
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLog.e("TAG", "connectionStatusJudgment=${state}")
        if (performConnectionOperations && state != "Connected") {
            //vpn连接失败
            KLog.d(logTagUf, "vpn连接失败")
            UnLimitedUtils.getBuriedPointUf("unlimF_vF")
            ToastUtils.toast(getString(R.string.connected_failed))
        }
        when (state) {
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
            }
            "Stopped" -> {
                disconnectServerSuccessful()
            }
        }
    }

    /**
     * 连接服务器成功
     */
    private fun connectionServerSuccessful() {
        binding.vpnState = 2
        changeOfVpnStatus()
        UnLimitedUtils.getBuriedPointUf("unlimF_vT")
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        KLog.e("TAG", "断开服务器")
        binding.vpnState = 0
        changeOfVpnStatus()
        if (clickToConnect) {
            UnLimitedUtils.getBuriedPointConnectionTimeUf(
                "unlimF_cn",
                mmkvUf.decodeInt(Constant.LAST_TIME_SECOND)
            )
        }
    }

    /**
     * vpn状态变化
     * 是否连接
     */
    private fun changeOfVpnStatus() {
        when (binding.vpnState) {
            0 -> {
                binding.imgState.setImageResource(R.mipmap.ic_diss_connect)
                binding.imgConnectionStatus.text = getString(R.string.connect)
                binding.txtTimerUf.text = getString(R.string._00_00_00)
                binding.txtTimerUf.setTextColor(getColor(R.color.tv_time_dis))
                UfTimerThread.endTiming()
                binding.lavViewUf.pauseAnimation()
                binding.lavViewUf.visibility = View.GONE
            }
            1 -> {
                if(!performConnectionOperations){
                    binding.imgConnectionStatus.text = getString(R.string.connecting)
                }else{
                    binding.imgConnectionStatus.text = getString(R.string.disconnecting)
                }
                binding.imgState.visibility = View.GONE
                binding.lavViewUf.visibility = View.VISIBLE
                binding.lavViewUf.playAnimation()
            }
            2 -> {
                binding.imgState.setImageResource(R.mipmap.ic_connect)
                binding.imgConnectionStatus.text = getString(R.string.disconnect)
                binding.txtTimerUf.setTextColor(getColor(R.color.tv_time_connect))
                UfTimerThread.startTiming()
                binding.lavViewUf.pauseAnimation()
                binding.lavViewUf.visibility = View.GONE
            }
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state)
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            if (App.nativeAdRefreshUf) {
                UfLoadHomeAd.getInstance().whetherToShowUf = false
                if (UfLoadHomeAd.getInstance().appAdDataUf != null) {
                    KLog.d(logTagUf, "onResume------>1")
                    UfLoadHomeAd.getInstance().setDisplayHomeNativeAdUf(this@MainActivity, binding)
                } else {
                    binding.vpnAdUf = false
                    KLog.d(logTagUf, "onResume------>2")
                    UfLoadHomeAd.getInstance().advertisementLoadingUf(this@MainActivity)
                    initHomeAd()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveEventBus
            .get(Constant.PLUG_UF_ADVERTISEMENT_SHOW, Boolean::class.java)
            .removeObserver {}
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStartUf?.cancel()
        jobStartUf = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            val serviceData = toJson(viewModel.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return true
    }
}