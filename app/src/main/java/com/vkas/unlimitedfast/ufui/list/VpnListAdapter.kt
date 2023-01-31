package com.vkas.unlimitedfast.ufui.list

import androidx.appcompat.app.AppCompatActivity
import com.chad.library.adapter.base.BaseDelegateMultiAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.delegate.BaseMultiTypeDelegate
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.unlimitedfast.R
import com.vkas.unlimitedfast.enevt.Constant
import com.vkas.unlimitedfast.ufbean.UfVpnBean
import com.vkas.unlimitedfast.ufutils.UnLimitedUtils.getFlagThroughCountryUf
import kotlinx.coroutines.Job

class VpnListAdapter(data: MutableList<UfVpnBean>?) :
    BaseQuickAdapter<UfVpnBean, BaseViewHolder>(
        R.layout.item_service,
        data
    ) {

    override fun convert(holder: BaseViewHolder, item: UfVpnBean) {
        if (item?.uf_best == true) {
            holder.setText(R.id.txt_country, Constant.FASTER_UF_SERVER)
            holder.setImageResource(
                R.id.img_flag,
                getFlagThroughCountryUf(Constant.FASTER_UF_SERVER)
            )
        } else {
            holder.setText(R.id.txt_country, item?.uf_country + "-" + item?.uf_city)
            holder.setImageResource(
                R.id.img_flag,
                getFlagThroughCountryUf(item?.uf_country.toString())
            )
        }

        if (item?.uf_check == true) {
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_connect)
            holder.setTextColor(R.id.txt_country, context.resources.getColor(R.color.white))
        } else {
            holder.setBackgroundResource(R.id.con_item, R.color.white)
            holder.setTextColor(R.id.txt_country, context.resources.getColor(R.color.tv_ff_333333))
        }
    }
}