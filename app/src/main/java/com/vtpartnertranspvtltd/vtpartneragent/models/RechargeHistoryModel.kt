package com.vtpartnertranspvtltd.vtpartneragent.models

import org.json.JSONObject

data class RechargeHistoryModel(
    val paymentId: String,
    val rechargeDate: String,
    val points: Double,
    val amount: Double,
    val lastRechargeNegativePoints: Double = 0.0
) {
    companion object {
        fun fromJson(json: JSONObject): RechargeHistoryModel {
            return RechargeHistoryModel(
                paymentId = json.getString("payment_id"),
                rechargeDate = json.getString("date"),
                points = json.getDouble("allotted_points"),
                amount = json.getDouble("amount"),
                lastRechargeNegativePoints = json.optDouble("last_recharge_negative_points", 0.0)
            )
        }
    }
}