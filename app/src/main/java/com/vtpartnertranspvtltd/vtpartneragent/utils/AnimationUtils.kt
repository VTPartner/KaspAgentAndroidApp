package com.vtpartnertranspvtltd.vtpartneragent.utils

import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.vtpartnertranspvtltd.vtpartneragent.R

object AnimationUtils {
    fun setFallDownAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = controller
        recyclerView.scheduleLayoutAnimation()
    }
} 