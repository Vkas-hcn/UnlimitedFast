package com.vkas.unlimitedfast.ufui.main

import android.app.Application
import com.vkas.unlimitedfast.ufbase.BaseViewModel
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.google.gson.reflect.TypeToken
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufapp.App.Companion.mmkvUf
import com.vkas.unlimitedfast.ufbean.UfIpBean
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.MmkvUtils
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.net.JsonUtil
class MainViewModel (application: Application) : BaseViewModel(application){
    //初始化服务器数据
    val liveInitializeServerData: MutableLiveData<UfVpnBean> by lazy {
        MutableLiveData<UfVpnBean>()
    }
    //更新服务器数据(未连接)
    val liveNoUpdateServerData: MutableLiveData<UfVpnBean> by lazy {
        MutableLiveData<UfVpnBean>()
    }
    //更新服务器数据(已连接)
    val liveUpdateServerData: MutableLiveData<UfVpnBean> by lazy {
        MutableLiveData<UfVpnBean>()
    }

    //当前服务器
    var currentServerData: UfVpnBean = UfVpnBean()
    //断开后选中服务器
    var afterDisconnectionServerData: UfVpnBean = UfVpnBean()
    //跳转结果页
    val liveJumpResultsPage: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }
    fun initializeServerData() {
        val bestData = UnLimitedUtils.getFastIpUf()
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                ProfileManager.updateProfile(setSkServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setSkServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
        currentServerData = bestData
        val serviceData = JsonUtil.toJson(currentServerData)
        MmkvUtils.set("currentServerData",serviceData)
        liveInitializeServerData.postValue(bestData)
    }

    fun updateSkServer(skServiceBean: UfVpnBean,isConnect:Boolean) {
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setSkServerData(it, skServiceBean)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        if(isConnect){
            afterDisconnectionServerData = skServiceBean
            liveUpdateServerData.postValue(skServiceBean)
        }else{
            currentServerData = skServiceBean
            val serviceData = JsonUtil.toJson(currentServerData)
            MmkvUtils.set("currentServerData",serviceData)
            liveNoUpdateServerData.postValue(skServiceBean)
        }
    }

    /**
     * 设置服务器数据
     */
    private fun setSkServerData(profile: Profile, bestData: UfVpnBean): Profile {
        profile.name = bestData.uf_country + "-" + bestData.uf_city
        profile.host = bestData.uf_ip.toString()
        profile.password = bestData.uf_pwd!!
        profile.method = bestData.uf_method!!
        profile.remotePort = bestData.uf_port!!
        return profile
    }
    /**
     * 跳转连接结果页
     */
    fun jumpConnectionResultsPage(isConnection: Boolean){
        val bundle = Bundle()
        val serviceData = mmkvUf.decodeString("currentServerData", "").toString()
        bundle.putBoolean(Constant.CONNECTION_UF_STATUS, isConnection)
        bundle.putString(Constant.SERVER_UF_INFORMATION, serviceData)
        liveJumpResultsPage.postValue(bundle)
    }

    /**
     * 解析是否是非法ip；中国大陆ip、伊朗ip
     */
    fun whetherParsingIsIllegalIp(): Boolean {
        val data = mmkvUf.decodeString(Constant.IP_INFORMATION)
        KLog.e("state","data=${data}===isNullOrEmpty=${Utils.isNullOrEmpty(data)}")
        return if (Utils.isNullOrEmpty(data)) {
            false
        } else {
            val ptIpBean: UfIpBean = JsonUtil.fromJson(
                mmkvUf.decodeString(Constant.IP_INFORMATION),
                object : TypeToken<UfIpBean?>() {}.type
            )
            return ptIpBean.country_code == "CN"
        }
    }

    /**
     * 是否显示不能使用弹框
     */
    fun whetherTheBulletBoxCannotBeUsed(context: AppCompatActivity) {
        val dialogVpn: AlertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.vpn))
            .setMessage(context.getString(R.string.cant_user_vpn))
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                XUtil.exitApp()
            }.create()
        dialogVpn.setCancelable(false)
        dialogVpn.show()
        dialogVpn.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialogVpn.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }
}