package com.vkas.unlimitedfast.ufui.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufapp.App.Companion.mmkvUf
import com.vkas.unlimitedfast.ufbase.BaseViewModel
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getFastIpUf
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getLocalServerData
import com.xuexiang.xui.utils.Utils.isNullOrEmpty
import com.xuexiang.xutil.net.JsonUtil

class VpnListViewModel (application: Application) : BaseViewModel(application) {
    private lateinit var skServiceBean : UfVpnBean
    private lateinit var skServiceBeanList :MutableList<UfVpnBean>

    // 服务器列表数据
    val liveServerListData: MutableLiveData<MutableList<UfVpnBean>> by lazy {
        MutableLiveData<MutableList<UfVpnBean>>()
    }

    /**
     * 获取服务器列表
     */
    fun getServerListData(){
        skServiceBeanList = ArrayList()
        skServiceBean = UfVpnBean()
        skServiceBeanList = if (isNullOrEmpty(mmkvUf.decodeString(Constant.PROFILE_UF_DATA))) {
            KLog.e("TAG","skServiceBeanList--1--->")
            getLocalServerData()
        } else {
            KLog.e("TAG","skServiceBeanList--2--->")

            JsonUtil.fromJson(
                mmkvUf.decodeString(Constant.PROFILE_UF_DATA),
                object : TypeToken<MutableList<UfVpnBean>?>() {}.type
            )
        }
        skServiceBeanList.add(0, getFastIpUf())
        KLog.e("LOG","skServiceBeanList---->${JsonUtil.toJson(skServiceBeanList)}")

        liveServerListData.postValue(skServiceBeanList)
    }
}