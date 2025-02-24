package com.vtpartnertranspvtltd.vt_partner.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import kotlin.math.abs

class GradientGenerator {
    companion object {
        private val baseColors = listOf(
            "#E8F5E9" to "#C8E6C9", // Green
            "#E3F2FD" to "#BBDEFB", // Blue
            "#FFF3E0" to "#FFE0B2", // Orange
            "#F3E5F5" to "#E1BEE7", // Purple
            "#E0F7FA" to "#B2EBF2", // Cyan
            "#FFF8E1" to "#FFE082", // Yellow
            "#FFEBEE" to "#FFCDD2"  // Red
        )

        fun generateGradient(position: Int): GradientDrawable {
            // Generate unique colors based on position
            val hueShift = (position * 137.5f) % 360 // Golden angle approximation
            val index = position % baseColors.size
            val (startColorStr, endColorStr) = baseColors[index]

            // Parse base colors
            val startColor = Color.parseColor(startColorStr)
            val endColor = Color.parseColor(endColorStr)

            // Shift hue while maintaining pastel nature
            val shiftedStartColor = shiftHue(startColor, hueShift)
            val shiftedEndColor = shiftHue(endColor, hueShift)

            return GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(shiftedStartColor, shiftedEndColor)
            ).apply {
                cornerRadius = 40f  // 12dp converted to pixels
                gradientType = GradientDrawable.LINEAR_GRADIENT
            }
        }

        private fun shiftHue(color: Int, hueShift: Float): Int {
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            
            // Shift hue
            hsv[0] = (hsv[0] + hueShift) % 360
            
            // Keep saturation low and value high for pastel colors
            hsv[1] = hsv[1].coerceIn(0.1f, 0.3f) // Low saturation
            hsv[2] = hsv[2].coerceIn(0.9f, 1.0f) // High value
            
            return Color.HSVToColor(hsv)
        }
    }
} 