package com.vtpartnertranspvtltd.vtpartneragent.models

import org.json.JSONObject

data class DriverDetails(
    val goodsDriverId: String,
    val driverFirstName: String,
    val driverLastName: String,
    val profilePic: String,
    val isOnline: Boolean,
    val ratings: Double,
    val mobileNo: String,
    val registrationDate: String,
    val time: String,
    val rLat: Double,
    val rLng: Double,
    val currentLat: Double,
    val currentLng: Double,
    val status: String,
    val recentOnlinePic: String?,
    val isVerified: Boolean,
    val categoryId: String,
    val vehicleId: String,
    val cityId: String,
    val aadharNo: String,
    val panCardNo: String,
    val houseNo: String,
    val cityName: String,
    val fullAddress: String,
    val gender: String,
    val ownerId: String,
    val aadharCardFront: String,
    val aadharCardBack: String,
    val panCardFront: String,
    val panCardBack: String,
    val licenseFront: String,
    val licenseBack: String,
    val insuranceImage: String,
    val nocImage: String,
    val pollutionCertificateImage: String,
    val rcImage: String,
    val driverVehicleImage: String,
    val vehiclePlateImage: String,
    val drivingLicenseNo: String,
    val vehiclePlateNo: String,
    val rcNo: String,
    val insuranceNo: String,
    val nocNo: String,
    val vehicleFuelType: String,
    val authtoken: String,
    val otpNo: String,
    val vehicleName: String,
    val vehicleWeight: String,
    val vehicleDescription: String,
    val vehicleImage: String,
    val vehicleSizeImage: String,
    val vehicleTypeName: String,
    val reason: String?
) {
    companion object {
        fun fromJson(json: JSONObject): DriverDetails {
            return DriverDetails(
                goodsDriverId = json.getString("goods_driver_id"),
                driverFirstName = json.getString("driver_first_name"),
                driverLastName = json.getString("driver_last_name"),
                profilePic = json.getString("profile_pic"),
                isOnline = json.getInt("is_online") == 1,
                ratings = json.getDouble("ratings"),
                mobileNo = json.getString("mobile_no"),
                registrationDate = json.getString("registration_date"),
                time = json.getString("time"),
                rLat = json.getDouble("r_lat"),
                rLng = json.getDouble("r_lng"),
                currentLat = json.getDouble("current_lat"),
                currentLng = json.getDouble("current_lng"),
                status = json.getString("status"),
                recentOnlinePic = json.optString("recent_online_pic"),
                isVerified = json.getInt("is_verified") == 1,
                categoryId = json.getString("category_id"),
                vehicleId = json.getString("vehicle_id"),
                cityId = json.getString("city_id"),
                aadharNo = json.getString("aadhar_no"),
                panCardNo = json.getString("pan_card_no"),
                houseNo = json.getString("house_no"),
                cityName = json.getString("city_name"),
                fullAddress = json.getString("full_address"),
                gender = json.getString("gender"),
                ownerId = json.getString("owner_id"),
                aadharCardFront = json.getString("aadhar_card_front"),
                aadharCardBack = json.getString("aadhar_card_back"),
                panCardFront = json.getString("pan_card_front"),
                panCardBack = json.getString("pan_card_back"),
                licenseFront = json.getString("license_front"),
                licenseBack = json.getString("license_back"),
                insuranceImage = json.getString("insurance_image"),
                nocImage = json.getString("noc_image"),
                pollutionCertificateImage = json.getString("pollution_certificate_image"),
                rcImage = json.getString("rc_image"),
                driverVehicleImage = json.getString("driver_vehicle_image"),
                vehiclePlateImage = json.getString("vehicle_plate_image"),
                drivingLicenseNo = json.getString("driving_license_no"),
                vehiclePlateNo = json.getString("vehicle_plate_no"),
                rcNo = json.getString("rc_no"),
                insuranceNo = json.getString("insurance_no"),
                nocNo = json.getString("noc_no"),
                vehicleFuelType = json.getString("vehicle_fuel_type"),
                authtoken = json.getString("authtoken"),
                otpNo = json.getString("otp_no"),
                vehicleName = json.getString("vehicle_name"),
                vehicleWeight = json.getString("vehicle_weight"),
                vehicleDescription = json.getString("vehicle_description"),
                vehicleImage = json.getString("vehicle_image"),
                vehicleSizeImage = json.getString("vehicle_size_image"),
                vehicleTypeName = json.getString("vehicle_type_name"),
                reason = json.optString("reason")
            )
        }
    }

    // Helper function to create Document objects for the UI
    fun toDocumentsList(): List<Document> {
        return listOf(
            Document("1", "Aadhar Front", aadharCardFront, DocumentType.AADHAR_FRONT),
            Document("2", "Aadhar Back", aadharCardBack, DocumentType.AADHAR_BACK),
            Document("3", "PAN Front", panCardFront, DocumentType.PAN_FRONT),
            Document("4", "PAN Back", panCardBack, DocumentType.PAN_BACK),
            Document("5", "License Front", licenseFront, DocumentType.LICENSE_FRONT),
            Document("6", "License Back", licenseBack, DocumentType.LICENSE_BACK),
            Document("7", "Insurance", insuranceImage, DocumentType.INSURANCE),
            Document("8", "NOC", nocImage, DocumentType.NOC),
            Document("9", "Pollution Certificate", pollutionCertificateImage, DocumentType.POLLUTION),
            Document("10", "RC", rcImage, DocumentType.RC),
            Document("11", "Vehicle", driverVehicleImage, DocumentType.VEHICLE),
            Document("12", "Vehicle Plate", vehiclePlateImage, DocumentType.VEHICLE_PLATE)
        )
    }

    // Helper function to get full name
    fun getFullName(): String {
        return "$driverFirstName $driverLastName".trim()
    }

    // Helper function to check if all documents are uploaded
    fun hasAllDocuments(): Boolean {
        return listOf(
            aadharCardFront, aadharCardBack, panCardFront, panCardBack,
            licenseFront, licenseBack, insuranceImage, nocImage,
            pollutionCertificateImage, rcImage, driverVehicleImage, vehiclePlateImage
        ).none { it.isNullOrEmpty() }
    }

    // Helper function to check verification status
    fun getVerificationStatus(): String {
        return when {
            isVerified -> "Verified"
            !hasAllDocuments() -> "Documents Pending"
            else -> "Under Review"
        }
    }
}