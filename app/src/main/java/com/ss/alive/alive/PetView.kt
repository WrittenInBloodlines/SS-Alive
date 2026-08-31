package com.ss.alive.alive

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView

class PetView(context: Context) : AppCompatTextView(context) {
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    private val handler = Handler(Looper.getMainLooper())
    private val behavior = PetBehavior(speedPxPerTick = 4)
    private var originalPet = "🐈"

    private val walkRunnable = object : Runnable {
        override fun run() {
            if (!dragging) movePet()
            handler.postDelayed(this, 30L)
        }
    }

    init {
        text = originalPet
        textSize = 64f
        gravity = android.view.Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(walkRunnable)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private fun movePet() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val position = behavior.step(
            currentX = params.x,
            currentY = params.y,
            petWidth = width.coerceAtLeast(params.width),
            petHeight = height.coerceAtLeast(params.height),
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )

        if (position.x != params.x || position.y != params.y) {
            params.x = position.x
            params.y = position.y
            windowManager.updateViewLayout(this, params)
        }
    }

    private fun reactToTap() {
        // A tap must reverse while the pet is still WALKING.
        // HELD is only entered once the finger actually starts dragging.
        behavior.reverse()
        text = "😺"
        handler.postDelayed({
            if (!isAttachedToWindow) return@postDelayed
            text = originalPet
            behavior.finishReverse()
        }, 220L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParams as? WindowManager.LayoutParams ?: return true
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                startX = params.x
                startY = params.y
                dragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY

                if (!dragging && (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8)) {
                    dragging = true
                    behavior.setHeld()
                }

                if (dragging) {
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    windowManager.updateViewLayout(this, params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    reactToTap()
                } else {
                    behavior.startFalling()
                }
                dragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (dragging) behavior.startFalling()
                dragging = false
                return true
            }
        }
        return true
    }
}
