package com.vkas.unlimitedfast.ufui.list

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.unlimitedfast.BR
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.databinding.ActivityListUfBinding
import com.vkas.unlimitedfast.databinding.ActivityStartBinding
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufbase.BaseActivity
import com.vkas.unlimitedfast.ufbase.BaseViewModel
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.vkas.unlimitedfast.ufutils.KLog
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job

class VpnListActivity : BaseActivity<ActivityListUfBinding, VpnListViewModel>() {
    private lateinit var selectAdapter: VpnListAdapter
    private var ufServiceBeanList: MutableList<UfVpnBean> = ArrayList()
    private lateinit var adBean: UfVpnBean

    private var jobBackUf: Job? = null

    //选中服务器
    private lateinit var checkSkServiceBean: UfVpnBean
    private lateinit var checkSkServiceBeanClick: UfVpnBean

    // 是否连接
    private var whetherToConnect = false
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_list_uf
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        checkSkServiceBean = UfVpnBean()
        whetherToConnect = bundle?.getBoolean(Constant.WHETHER_UF_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Constant.CURRENT_UF_SERVICE),
            object : TypeToken<UfVpnBean?>() {}.type
        )
        checkSkServiceBeanClick = checkSkServiceBean
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.selectTitleUf.tvTitle.text = getString(R.string.locations)
        binding.selectTitleUf.tvRight.visibility = View.GONE
        binding.selectTitleUf.imgBack.setOnClickListener {
            returnToHomePage()
        }
    }

    override fun initData() {
        super.initData()
        initSelectRecyclerView()
        viewModel.getServerListData()
//        UfLoadBackAd.getInstance().whetherToShowUf = false
    }

    override fun initViewObservable() {
        super.initViewObservable()
        getServerListData()
    }

    private fun getServerListData() {
        viewModel.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = VpnListAdapter(ufServiceBeanList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.recyclerSelect.layoutManager = layoutManager
        binding.recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener { _, _, pos ->
            run {
                selectServer(pos)
            }
        }
    }


    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        if (ufServiceBeanList[position].uf_ip == checkSkServiceBeanClick.uf_ip && ufServiceBeanList[position].uf_best == checkSkServiceBeanClick.uf_best) {
            if (!whetherToConnect) {
                finish()
                LiveEventBus.get<UfVpnBean>(Constant.NOT_CONNECTED_UF_RETURN)
                    .post(checkSkServiceBean)
            }
            return
        }
        ufServiceBeanList.forEachIndexed { index, _ ->
            ufServiceBeanList[index].uf_check = position == index
            if (ufServiceBeanList[index].uf_check == true) {
                checkSkServiceBean = ufServiceBeanList[index]
            }
        }
        selectAdapter.notifyDataSetChanged()
        showDisconnectDialog()
    }

    /**
     * 回显服务器
     */
    private fun echoServer(it: MutableList<UfVpnBean>) {
        ufServiceBeanList = it
        ufServiceBeanList.forEachIndexed { index, _ ->
            if (checkSkServiceBeanClick.uf_best == true) {
                ufServiceBeanList[0].uf_check = true
            } else {
                ufServiceBeanList[index].uf_check =
                    ufServiceBeanList[index].uf_ip == checkSkServiceBeanClick.uf_ip
                ufServiceBeanList[0].uf_check = false
            }
        }
        KLog.e("TAG", "ufServiceBeanList=${JsonUtil.toJson(ufServiceBeanList)}")
        selectAdapter.setList(ufServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        finish()
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            LiveEventBus.get<UfVpnBean>(Constant.NOT_CONNECTED_UF_RETURN)
                .post(checkSkServiceBean)
            return
        }
        val dialog: AlertDialog? = AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCUF") { dialog, _ ->
                dialog.dismiss()
                ufServiceBeanList.forEachIndexed { index, _ ->
                    ufServiceBeanList[index].uf_check =
                        (ufServiceBeanList[index].uf_ip == checkSkServiceBeanClick.uf_ip && ufServiceBeanList[index].uf_best == checkSkServiceBeanClick.uf_best)
                }
                selectAdapter.notifyDataSetChanged()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<UfVpnBean>(Constant.CONNECTED_UF_RETURN)
                    .post(checkSkServiceBean)
            }.create()

        val params = dialog!!.window!!.attributes
        params.width = 200
        params.height = 200
        dialog.window!!.attributes = params
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToHomePage()
        }
        return true
    }
}