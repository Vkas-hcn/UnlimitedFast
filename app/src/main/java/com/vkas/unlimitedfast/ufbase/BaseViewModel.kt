package com.vkas.unlimitedfast.ufbase

import android.app.Application
import androidx.lifecycle.MutableLiveData
open class BaseViewModel (application: Application) : BaseViewModelMVVM(application) {
    var stateLiveData: StateLiveData<Any> = StateLiveData()
    fun getStateLiveData(): MutableLiveData<Any> {
        return stateLiveData
    }
}