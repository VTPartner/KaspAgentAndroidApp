package com.vtpartnertranspvtltd.vtpartneragent.models

data class Document(
    val id: String,
    val name: String,
    val imageUrl: String,
    val type: DocumentType
)

enum class DocumentType {
    AADHAR_FRONT, AADHAR_BACK, PAN_FRONT, PAN_BACK, LICENSE_FRONT, LICENSE_BACK,
    INSURANCE, NOC, POLLUTION, RC, VEHICLE, VEHICLE_PLATE,PROFILE
}