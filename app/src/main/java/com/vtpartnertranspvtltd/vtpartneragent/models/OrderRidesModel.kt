import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject

data class OrderRidesModel(
    val customerName: String,
    val bookingTiming: Double,
    val totalPrice: Double,
    val pickupAddress: String,
    val dropAddress: String,
    val customerImage: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        customerName = parcel.readString() ?: "",
        bookingTiming = parcel.readDouble(),
        totalPrice = parcel.readDouble(),
        pickupAddress = parcel.readString() ?: "",
        dropAddress = parcel.readString() ?: "",
        customerImage = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(customerName)
        parcel.writeDouble(bookingTiming)
        parcel.writeDouble(totalPrice)
        parcel.writeString(pickupAddress)
        parcel.writeString(dropAddress)
        parcel.writeString(customerImage)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<OrderRidesModel> = object : Parcelable.Creator<OrderRidesModel> {
            override fun createFromParcel(parcel: Parcel): OrderRidesModel {
                return OrderRidesModel(parcel)
            }

            override fun newArray(size: Int): Array<OrderRidesModel?> {
                return arrayOfNulls(size)
            }
        }

        fun fromJson(json: JSONObject): OrderRidesModel {
            return OrderRidesModel(
                customerName = json.getString("customer_name"),
                bookingTiming = json.getString("booking_timing").toDouble(),
                totalPrice = json.getString("total_price").toDouble(),
                pickupAddress = json.getString("pickup_address"),
                dropAddress = json.getString("drop_address"),
                customerImage = json.optString("customer_image")
            )
        }
    }
}