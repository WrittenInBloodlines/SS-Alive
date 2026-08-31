package com.ss.alive.alive

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.abs

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
        includeFontPadding = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAppearance()
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
        val petWidth = measuredWidth.coerceAtLeast(width).coerceAtLeast(params.width)
        val petHeight = measuredHeight.coerceAtLeast(height).coerceAtLeast(params.height)

        val position = behavior.step(
            currentX = params.x,
            currentY = params.y,
            petWidth = petWidth,
            petHeight = petHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )

        updateAppearance()

        if (position.x != params.x || position.y != params.y) {
            params.x = position.x
            params.y = position.y
            windowManager.updateViewLayout(this, params)
        }
    }

    private fun updateAppearance() {
        text = when (behavior.state) {
            PetBehavior.State.WALKING -> originalPet
            PetBehavior.State.IDLE -> "😺"
            PetBehavior.State.FALLING -> "🙀"
            PetBehavior.State.LANDING -> "😵"
            PetBehavior.State.HELD -> originalPet
        }
        scaleX = if (behavior.direction < 0) -1f else 1f
    }

    private fun reactToTap() {
        behavior.reverse()
        updateAppearance()
        handler.postDelayed({
            if (!isAttachedToWindow) return@postDelayed
            behavior.finishReverse()
            updateAppearance()
        }, 220L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParams as? WindowManager.LayoutParams ?: return true
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val petWidth = measuredWidth.coerceAtLeast(width).coerceAtLeast(params.width)
        val petHeight = measuredHeight.coerceAtLeast(height).coerceAtLeast(params.height)
        val maxX = (screenWidth - petWidth).coerceAtLeast(0)
        val maxY = (screenHeight - petHeight).coerceAtLeast(0)

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

                if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) {
                    dragging = true
                    behavior.setHeld()
                    updateAppearance()
                }

                if (dragging) {
                    params.x = (startX + dx.toInt()).coerceIn(0, maxX)
                    params.y = (startY + dy.toInt()).coerceIn(0, maxY)
                    windowManager.updateViewLayout(this, params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    reactToTap()
                } else {
                    behavior.startFalling()
                    updateAppearance()
                }
                dragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    behavior.startFalling()
                    updateAppearance()
                }
                dragging = false
                return true
            }
        }
        return true
    }
}
