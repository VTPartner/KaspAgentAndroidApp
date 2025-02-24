import androidx.annotation.DrawableRes

data class DrawerItem(
    val id: Int,
    @DrawableRes val iconRes: Int,
    val title: String,
    val hasBadge: Boolean = false
) 