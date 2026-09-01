package com.ss.alive.alive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class PetView(context: Context, private val profile: AliveProfile) : View(context) {
    private var downX = 0f; private var downY = 0f; private var startX = 0; private var startY = 0; private var dragging = false
    private val handler = Handler(Looper.getMainLooper())
    private val behavior = PetBehavior(speedPxPerTick = 4)
    private val customSprite = CustomPetSprite(context, profile)
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private var lastState = behavior.state
    private var lastDirection = behavior.direction

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (!dragging) movePet()
            updateSpriteFrame(); invalidate(); handler.postDelayed(this, 30L)
        }
    }

    init { setBackgroundColor(Color.TRANSPARENT); isClickable = true; isFocusable = false; setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); lastAnimationTime = System.currentTimeMillis(); handler.post(animationRunnable) }
    override fun onDetachedFromWindow() { handler.removeCallbacksAndMessages(null); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); customSprite.draw(canvas, behavior.state, animationFrame, behavior.direction) }

    private fun movePet() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenWidth = resources.displayMetrics.widthPixels; val screenHeight = resources.displayMetrics.heightPixels
        val petWidth = measuredWidth.coerceAtLeast(width).coerceAtLeast(params.width)
        val petHeight = measuredHeight.coerceAtLeast(height).coerceAtLeast(params.height)
        val position = behavior.step(params.x, params.y, petWidth, petHeight, screenWidth, screenHeight)
        if (position.x != params.x || position.y != params.y) { params.x = position.x; params.y = position.y; runCatching { wm.updateViewLayout(this, params) } }
    }

    private fun updateSpriteFrame() {
        val now = System.currentTimeMillis()
        if (behavior.state != lastState || behavior.direction != lastDirection) {
            animationFrame = 0; lastAnimationTime = now; lastState = behavior.state; lastDirection = behavior.direction; return
        }
        val duration = when (behavior.state) {
            PetBehavior.State.RUNNING -> 65L
            PetBehavior.State.WALKING -> 90L
            PetBehavior.State.IDLE -> 150L
            PetBehavior.State.FALLING -> 75L
            PetBehavior.State.LANDING -> 70L
            PetBehavior.State.HELD -> 150L
            PetBehavior.State.SIT -> 190L
            PetBehavior.State.JUMP -> 80L
        }
        val count = customSprite.frameCount(behavior.state).coerceAtLeast(1)
        if (now - lastAnimationTime >= duration) {
            val elapsed = ((now - lastAnimationTime) / duration).toInt().coerceAtMost(count)
            animationFrame = (animationFrame + elapsed) % count
            lastAnimationTime = now
        }
    }

    private fun reactToTap() {
        behavior.reverse(); animationFrame = 0; invalidate()
        handler.postDelayed({ if (!isAttachedToWindow) return@postDelayed; behavior.finishReverse(); animationFrame = 0; invalidate() }, 220L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParams as? WindowManager.LayoutParams ?: return true
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenWidth = resources.displayMetrics.widthPixels; val screenHeight = resources.displayMetrics.heightPixels
        val petWidth = measuredWidth.coerceAtLeast(width).coerceAtLeast(params.width); val petHeight = measuredHeight.coerceAtLeast(height).coerceAtLeast(params.height)
        val maxX = (screenWidth - petWidth).coerceAtLeast(0); val maxY = (screenHeight - petHeight).coerceAtLeast(0)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; dragging = false; return true }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX; val dy = event.rawY - downY
                if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) { dragging = true; behavior.setHeld(); animationFrame = 0; invalidate() }
                if (dragging) { params.x = (startX + dx.toInt()).coerceIn(0, maxX); params.y = (startY + dy.toInt()).coerceIn(0, maxY); runCatching { wm.updateViewLayout(this, params) } }
                return true
            }
            MotionEvent.ACTION_UP -> { if (!dragging) reactToTap() else behavior.startFalling(); animationFrame = 0; invalidate(); dragging = false; return true }
            MotionEvent.ACTION_CANCEL -> { if (dragging) behavior.startFalling(); animationFrame = 0; invalidate(); dragging = false; return true }
        }
        return true
    }
}
