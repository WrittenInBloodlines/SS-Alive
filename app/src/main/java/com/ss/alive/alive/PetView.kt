package com.ss.alive.alive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class PetView(context: Context, private val profile: AliveProfile) : View(context) {
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private val handler = Handler(Looper.getMainLooper())
    private val behavior = PetBehavior()
    private val customSprite = CustomPetSprite(context, profile)
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private var lastState = behavior.state
    private var lastDirection = behavior.direction
    private var interactionUntil = 0L
    private var interactionAnimationUntil = 0L
    private var appSitUntil = 0L

    val profileId: String get() = profile.id
    val isBeingDragged: Boolean get() = dragging

    fun canStartInteraction(): Boolean {
        return profile.interactionEnabled &&
            profile.frameUris(AliveProfile.INTERACTION).isNotEmpty() &&
            !dragging &&
            behavior.state != PetBehavior.State.HELD &&
            behavior.state != PetBehavior.State.FALLING &&
            behavior.state != PetBehavior.State.JUMP &&
            behavior.state != PetBehavior.State.LANDING &&
            behavior.state != PetBehavior.State.INTERACTING &&
            behavior.state != PetBehavior.State.APP_SIT
    }

    fun interactionDurationMs(): Long {
        val frames = customSprite.frameCount(PetBehavior.State.INTERACTING).coerceAtLeast(1)
        return (frames * 1000L / profile.speedFps(AliveProfile.INTERACTION)).coerceAtLeast(300L)
    }

    /** Starts this Alive's own animation, synchronized to the other Alive's duration. */
    fun startInteraction(sharedDurationMs: Long): Boolean {
        if (!canStartInteraction()) return false
        val now = System.currentTimeMillis()
        val ownDuration = interactionDurationMs()
        interactionAnimationUntil = now + ownDuration
        interactionUntil = now + maxOf(ownDuration, sharedDurationMs)
        behavior.setInteracting()
        animationFrame = 0
        lastAnimationTime = now
        invalidate()
        return true
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            when {
                behavior.state == PetBehavior.State.INTERACTING -> {
                    if (now >= interactionUntil) {
                        interactionUntil = 0L
                        interactionAnimationUntil = 0L
                        behavior.resumeWalking()
                        animationFrame = 0
                    }
                }
                behavior.state == PetBehavior.State.APP_SIT -> {
                    if (now >= appSitUntil) {
                        behavior.resumeFromAppSitting()
                        appSitUntil = 0L
                        animationFrame = 0
                    }
                }
                !dragging -> movePet()
            }
            updateSpriteFrame()
            invalidate()
            handler.postDelayed(this, 30L)
        }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = false
        alpha = if (profile.ghostMode) 0.35f else 1f
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastAnimationTime = System.currentTimeMillis()
        handler.post(animationRunnable)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        customSprite.draw(canvas, behavior.state, animationFrame, behavior.direction)
    }

    private fun movePet() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val petWidth = measuredWidth.coerceAtLeast(width).coerceAtLeast(params.width)
        val petHeight = measuredHeight.coerceAtLeast(height).coerceAtLeast(params.height)
        val previousY = params.y
        val previousState = behavior.state
        val position = behavior.step(params.x, params.y, petWidth, petHeight, screenWidth, screenHeight)
        var nextX = position.x
        var nextY = position.y

        if (previousState == PetBehavior.State.FALLING && nextY > previousY) {
            val oldBottom = previousY + petHeight
            val newBottom = nextY + petHeight
            val landingSurface = AlivePlatformRegistry.snapshot()
                .asSequence()
                .filter { rect ->
                    rect.top > 0 &&
                        rect.left < params.x + petWidth &&
                        rect.right > params.x &&
                        oldBottom <= rect.top - 8 &&
                        newBottom >= rect.top
                }
                .minByOrNull { it.top }

            if (landingSurface != null) {
                nextY = (landingSurface.top - petHeight).coerceAtLeast(0)
                behavior.setAppSitting()
                appSitUntil = System.currentTimeMillis() + APP_SIT_DURATION_MS
                animationFrame = 0
            }
        }

        if (nextX != params.x || nextY != params.y) {
            params.x = nextX
            params.y = nextY
            runCatching { wm.updateViewLayout(this, params) }
        }
    }

    private fun updateSpriteFrame() {
        val now = System.currentTimeMillis()
        if (behavior.state != lastState || behavior.direction != lastDirection) {
            animationFrame = 0
            lastAnimationTime = now
            lastState = behavior.state
            lastDirection = behavior.direction
            return
        }
        val stateKey = when (behavior.state) {
            PetBehavior.State.WALKING -> AliveProfile.WALK
            PetBehavior.State.CLIMBING -> AliveProfile.CLIMB
            PetBehavior.State.IDLE -> AliveProfile.IDLE
            PetBehavior.State.RUNNING -> AliveProfile.RUN
            PetBehavior.State.SIT -> AliveProfile.SIT
            PetBehavior.State.JUMP -> AliveProfile.JUMP
            PetBehavior.State.FALLING -> AliveProfile.FALL
            PetBehavior.State.LANDING -> AliveProfile.LANDING
            PetBehavior.State.HELD -> AliveProfile.HELD
            PetBehavior.State.INTERACTING -> AliveProfile.INTERACTION
            PetBehavior.State.APP_SIT -> if (profile.frameUris(AliveProfile.APP_SIT).isNotEmpty()) AliveProfile.APP_SIT else AliveProfile.SIT
        }
        if (behavior.state == PetBehavior.State.INTERACTING && now >= interactionAnimationUntil) return

        val duration = (1000L / profile.speedFps(stateKey)).coerceAtLeast(16L)
        val count = customSprite.frameCount(behavior.state).coerceAtLeast(1)
        if (now - lastAnimationTime >= duration) {
            val elapsed = ((now - lastAnimationTime) / duration).toInt().coerceAtMost(count)
            animationFrame = if (behavior.state == PetBehavior.State.INTERACTING) {
                (animationFrame + elapsed).coerceAtMost(count - 1)
            } else {
                (animationFrame + elapsed) % count
            }
            lastAnimationTime = now
        }
    }

    private fun reactToTap() {
        if (behavior.state == PetBehavior.State.INTERACTING) return
        behavior.reverse()
        animationFrame = 0
        invalidate()
        handler.postDelayed({
            if (!isAttachedToWindow) return@postDelayed
            behavior.finishReverse()
            animationFrame = 0
            invalidate()
        }, 220L)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val params = layoutParams as? WindowManager.LayoutParams ?: return true
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
                    interactionUntil = 0L
                    interactionAnimationUntil = 0L
                    behavior.setHeld()
                    animationFrame = 0
                    invalidate()
                }
                if (dragging) {
                    params.x = (startX + dx.toInt()).coerceIn(0, maxX)
                    params.y = (startY + dy.toInt()).coerceIn(0, maxY)
                    runCatching { wm.updateViewLayout(this, params) }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) reactToTap() else behavior.startFalling()
                interactionUntil = 0L
                interactionAnimationUntil = 0L
                animationFrame = 0
                invalidate()
                dragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) behavior.startFalling()
                interactionUntil = 0L
                interactionAnimationUntil = 0L
                animationFrame = 0
                invalidate()
                dragging = false
                return true
            }
        }
        return true
    }

    companion object {
        private const val APP_SIT_DURATION_MS = 10000L
    }
}
