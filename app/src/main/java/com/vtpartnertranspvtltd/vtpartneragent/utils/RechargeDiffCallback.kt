package com.vtpartnertranspvtltd.vtpartneragent.utils

import androidx.recyclerview.widget.DiffUtil
import com.vtpartnertranspvtltd.vtpartneragent.models.RechargeModel

class RechargeDiffCallback(
    private val oldList: List<RechargeModel>,
    private val newList: List<RechargeModel>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].rechargeId == newList[newItemPosition].rechargeId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return old.amount == new.amount &&
                old.points == new.points &&
                old.validDays == new.validDays &&
                old.description == new.description
    }
}