package com.vtpartnertranspvtltd.vtpartneragent.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.vtpartnertranspvtltd.vtpartneragent.R

class OTPView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var otpLength = 6
    private var boxWidth = 45f
    private var boxHeight = 45f
    private var boxSpacing = 8f
    private var boxRadius = 5f
    private var boxStrokeWidth = 1f
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = boxStrokeWidth
    }

    private var onOtpSubmitListener: ((String) -> Unit)? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OTPView,
            0, 0
        ).apply {
            try {
                otpLength = getInteger(R.styleable.OTPView_otpLength, 6)
                boxWidth = getDimension(R.styleable.OTPView_boxWidth, 45f)
                boxHeight = getDimension(R.styleable.OTPView_boxHeight, 45f)
                boxSpacing = getDimension(R.styleable.OTPView_boxSpacing, 8f)
                boxRadius = getDimension(R.styleable.OTPView_boxRadius, 5f)
                boxStrokeWidth = getDimension(R.styleable.OTPView_boxStrokeWidth, 1f)
            } finally {
                recycle()
            }
        }

        setupView()
    }

    private fun setupView() {
        maxLines = 1
        filters = arrayOf(InputFilter.LengthFilter(otpLength))
        inputType = InputType.TYPE_CLASS_NUMBER
        background = null
        isCursorVisible = false
        setTextIsSelectable(false)
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }

        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == otpLength) {
                    onOtpSubmitListener?.invoke(s.toString())
                }
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = ((boxWidth + boxSpacing) * otpLength).toInt()
        val height = boxHeight.toInt()
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val totalWidth = width.toFloat()
        val startX = (totalWidth - (boxWidth * otpLength + boxSpacing * (otpLength - 1))) / 2

        for (i in 0 until otpLength) {
            val left = startX + i * (boxWidth + boxSpacing)
            val right = left + boxWidth
            val top = 0f
            val bottom = boxHeight

            // Draw box
            paint.color = if (i == text?.length ?: 0) {
                ContextCompat.getColor(context, R.color.primary)
            } else {
                ContextCompat.getColor(context, R.color.text_secondary)
            }
            canvas.drawRoundRect(left, top, right, bottom, boxRadius, boxRadius, paint)

            // Draw text
            if (i < text?.length ?: 0) {
                val textPaint = Paint().apply {
                    color = ContextCompat.getColor(context, R.color.black)
                    textSize = textSize
                    textAlign = Paint.Align.CENTER
                }
                val xPos = left + boxWidth / 2
                val yPos = (bottom + textPaint.textSize - textPaint.descent()) / 2
                canvas.drawText(text!![i].toString(), xPos, yPos, textPaint)
            }
        }
    }

    fun setOnOtpSubmit(listener: (String) -> Unit) {
        onOtpSubmitListener = listener
    }

    val otp: String
        get() = text?.toString() ?: ""
} 