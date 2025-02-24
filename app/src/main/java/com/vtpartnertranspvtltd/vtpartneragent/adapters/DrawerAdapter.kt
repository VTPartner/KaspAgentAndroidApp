import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vtpartnertranspvtltd.vtpartneragent.databinding.DrawerItemLayoutBinding

class DrawerAdapter(
    private val context: Context,
    private val items: List<DrawerItem>,
    private val onItemClick: (DrawerItem) -> Unit
) : RecyclerView.Adapter<DrawerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: DrawerItemLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DrawerItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            itemIcon.setImageResource(item.iconRes)
            itemText.text = item.title
            
            // Show badge if needed
            itemBadge.isVisible = item.hasBadge
            
            root.setOnClickListener {
                onItemClick(item)
                // Add ripple effect
                root.isPressed = true
                root.postDelayed({ root.isPressed = false }, 100)
            }
        }
    }

    override fun getItemCount() = items.size
} 