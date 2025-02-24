import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vtpartnertranspvtltd.vtpartneragent.services.BookingAcceptanceService

class BookingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bookingId = intent.getStringExtra("booking_id") ?: return

        when (intent.action) {
            "ACCEPT_BOOKING" -> {
                handleBookingAcceptance(context, bookingId)
            }
            "REJECT_BOOKING" -> {
                handleBookingRejection(context, bookingId)
            }
        }

        // Stop the BookingAcceptanceService
        context.stopService(Intent(context, BookingAcceptanceService::class.java))
    }

    private fun handleBookingAcceptance(context: Context, bookingId: String) {
        // Implement your booking acceptance logic here
    }

    private fun handleBookingRejection(context: Context, bookingId: String) {
        // Implement your booking rejection logic here
    }
} 