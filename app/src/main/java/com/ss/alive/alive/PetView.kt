package com.ss.alive.alive

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.widget.TextView
import android.view.WindowManager

class PetView(context: Context) : androidx.appcompat.widget.AppCompatTextView(context) {
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0

    init {
        text = "🐈"
        textSize = 64f
        gravity = android.view.Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParams as? WindowManager.LayoutParams ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = startX + (event.rawX - downX).toInt()
                params.y = startY + (event.rawY - downY).toInt()
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).updateViewLayout(this, params)
                return true
            }
        }
        return true
    }
}
