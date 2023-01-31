package com.vkas.unlimitedfast.ufui.webuf

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.vkas.unlimitedfast.BR
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.databinding.ActivityWebUfBinding
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufbase.BaseActivity
import com.vkas.unlimitedfast.ufbase.BaseViewModel

class WebUfActivity : BaseActivity<ActivityWebUfBinding, BaseViewModel>() {
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_web_uf
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.webTitleEl.imgBack.visibility = View.VISIBLE
        binding.webTitleEl.imgBack.setImageResource(R.drawable.ic_title_back_uf)

        binding.webTitleEl.imgBack.setOnClickListener {
            finish()
        }
        binding.webTitleEl.tvTitle.visibility = View.GONE
    }

    override fun initData() {
        super.initData()
        binding.ppWebEl.loadUrl(Constant.PRIVACY_UF_AGREEMENT)
        binding.ppWebEl.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            }

            override fun onPageFinished(view: WebView, url: String) {
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }
        }

        binding.ppWebEl.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler, error: SslError
            ) {
                handler.proceed()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (Constant.PRIVACY_UF_AGREEMENT == url) {
                    view.loadUrl(url)
                } else {
                    // 系统处理
                    return super.shouldOverrideUrlLoading(view, url)
                }
                return true
            }
        }


    }


    //点击返回上一页面而不是退出浏览器
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.ppWebEl.canGoBack()) {
            binding.ppWebEl.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        binding.ppWebEl.loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
        binding.ppWebEl.clearHistory()
        (binding.ppWebEl.parent as ViewGroup).removeView(binding.ppWebEl)
        binding.ppWebEl.destroy()
        super.onDestroy()
    }
}