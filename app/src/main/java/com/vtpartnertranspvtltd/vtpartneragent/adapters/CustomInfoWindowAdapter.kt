import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.vtpartnertranspvtltd.vtpartneragent.databinding.InfoWindowLayoutBinding

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {
    private val binding = InfoWindowLayoutBinding.inflate(LayoutInflater.from(context))

    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View {
        binding.apply {
            title.text = marker.title
            snippet.text = marker.snippet
        }
        return binding.root
    }
} 