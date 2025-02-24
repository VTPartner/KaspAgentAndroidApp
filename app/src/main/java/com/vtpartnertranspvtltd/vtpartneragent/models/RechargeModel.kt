package com.vtpartnertranspvtltd.vtpartneragent.models

import org.json.JSONObject

data class RechargeModel(
    val rechargeId: String,
    val amount: String,
    val points: String,
    val validDays: String,
    val description: String
) {
    companion object {
        fun fromJson(json: JSONObject): RechargeModel {
            return RechargeModel(
                rechargeId = json.getString("recharge_id"),
                amount = json.getString("amount"),
                points = json.getString("points"),
                validDays = json.getString("valid_days"),
                description = json.getString("description")
            )
        }
    }
}