package com.ss.alive.alive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
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
    private val behavior = PetBehavior(
        speedPxPerTick = (4f * profile.movementSpeedPercent.coerceIn(50, 150) / 100f *
            (0.75f + profile.energyLevel.coerceIn(0, 100) / 250f)).toInt().coerceIn(1, 8),
        idleChance = (0.002f + profile.sleepFrequency.coerceIn(0, 100) * 0.00003f -
            profile.energyLevel.coerceIn(0, 100) * 0.000015f).coerceIn(0.0005f, 0.005f),
        idleDurationMultiplier = 0.7f + profile.sleepFrequency.coerceIn(0, 100) / 100f * 1.5f,
        curiosityLevel = profile.curiosity,
        gravityMultiplier = profile.gravityStrengthPercent.coerceIn(50, 150) / 100f
    )
    private var interactionCooldownUntil = 0L
    private var activeSound: MediaPlayer? = null
    private val customSprite = CustomPetSprite(context, profile)
    private var animationFrame = 0
    private var lastAnimationTime = 0L
    private var lastState = behavior.state
    private var lastDirection = behavior.direction
    private var interactionUntil = 0L
    private var interactionAnimationUntil = 0L
    private var appSitUntil = 0L
    private var lastAppPlatformTop: Int? = null

    val profileId: String get() = profile.id
    val isBeingDragged: Boolean get() = dragging

    fun canStartInteraction(): Boolean {
        return System.currentTimeMillis() >= interactionCooldownUntil &&
            profile.interactionEnabled &&
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
        interactionCooldownUntil = now + 10000L + (100 - profile.interactionFrequency.coerceIn(0, 100)) * 400L
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
        activeSound?.release()
        activeSound = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        customSprite.draw(canvas, behavior.state, animationFrame, behavior.direction)
        profile.heldItem?.let { item ->
            val itemPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = (height * 0.23f).coerceAtLeast(24f)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            canvas.drawText(item, width * 0.76f, height * 0.78f, itemPaint)
        }
    }

    private fun movePet() {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val petWidth = measuredWidth.coerceAtLeast(width).coerceAtLeast(params.width)
        val petHeight = measuredHeight.coerceAtLeast(height).coerceAtLeast(params.height)
        val zonePrefs = context.getSharedPreferences("ss_alive_tools", Context.MODE_PRIVATE)
        val leftInset = (screenWidth * zonePrefs.getInt("left", 0).coerceIn(0, 35) / 100f).toInt()
        val rightInset = (screenWidth * zonePrefs.getInt("right", 0).coerceIn(0, 35) / 100f).toInt()
        val topInset = (screenHeight * zonePrefs.getInt("top", 0).coerceIn(0, 35) / 100f).toInt()
        val bottomInset = (screenHeight * zonePrefs.getInt("bottom", 0).coerceIn(0, 35) / 100f).toInt()
        val usableWidth = (screenWidth - leftInset - rightInset).coerceAtLeast(petWidth)
        val usableHeight = (screenHeight - topInset - bottomInset).coerceAtLeast(petHeight)
        val previousY = params.y
        val previousState = behavior.state
        val localPosition = behavior.step(
            (params.x - leftInset).coerceAtLeast(0),
            (params.y - topInset).coerceAtLeast(0),
            petWidth, petHeight, usableWidth, usableHeight
        )
        val position = PetBehavior.Position(localPosition.x + leftInset, localPosition.y + topInset)
        var nextX = position.x
        var nextY = position.y

        if (previousState == PetBehavior.State.FALLING && nextY > previousY) {
            val newBottom = nextY + petHeight
            val landingSurface = AlivePlatformRegistry.snapshot()
                .asSequence()
                .filter { rect ->
                    rect.top > 0 &&
                        rect.top != lastAppPlatformTop &&
                        rect.left < params.x + petWidth &&
                        rect.right > params.x &&
                        // A surface is valid only if the pet's bottom crosses its
                        // top edge while falling. This prevents snapping upward
                        // onto a control that is already beside/inside the pet.
                        rect.top >= previousY + petHeight - 8 &&
                        rect.top <= newBottom
                }
                .minByOrNull { it.top }

            if (landingSurface != null) {
                nextY = (landingSurface.top - petHeight).coerceAtLeast(0)
                lastAppPlatformTop = landingSurface.top
                AliveStatistics.recordLanding(context, profile.id)
                playConfiguredSound()
                behavior.setAppSitting()
                appSitUntil = System.currentTimeMillis() + APP_SIT_DURATION_MS
                animationFrame = 0
            }
        }
        val previousPlatformTop = lastAppPlatformTop
        if (previousState == PetBehavior.State.FALLING && previousPlatformTop != null &&
            previousY > previousPlatformTop
        ) {
            lastAppPlatformTop = null
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

    private fun reactToTap(localY: Float) {
        if (behavior.state == PetBehavior.State.INTERACTING) return
        AliveStatistics.recordTap(context, profile.id)
        playConfiguredSound()
        val reaction = if (localY <= height * 0.38f) profile.headTapReaction else profile.bodyTapReaction
        animationFrame = 0
        when (reaction) {
            "JUMP" -> behavior.jump()
            "SIT" -> {
                behavior.setSitting()
                handler.postDelayed({
                    if (!isAttachedToWindow) return@postDelayed
                    behavior.resumeWalking()
                    animationFrame = 0
                    invalidate()
                }, 1800L)
            }
            else -> {
                behavior.reverse()
                handler.postDelayed({
                    if (!isAttachedToWindow) return@postDelayed
                    behavior.finishReverse()
                    animationFrame = 0
                    invalidate()
                }, 220L)
            }
        }
        invalidate()
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
                    lastAppPlatformTop = null
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
                if (!dragging) reactToTap(event.y) else {
                    AlivePositionStore.save(context, profile.id, params.x, params.y)
                    behavior.startFalling()
                }
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

    private fun playConfiguredSound() {
        val soundUri = profile.soundUri
        if (!profile.soundsEnabled || soundUri.isNullOrBlank()) return
        runCatching {
            activeSound?.release()
            activeSound = MediaPlayer.create(context, Uri.parse(soundUri)).also { player ->
                if (player != null) {
                    val volume = profile.soundVolume.coerceIn(0, 100) / 100f
                    player.setVolume(volume, volume)
                    player.setOnCompletionListener {
                        it.release()
                        if (activeSound === it) activeSound = null
                    }
                    player.start()
                }
            }
        }
    }

    companion object {
        private const val APP_SIT_DURATION_MS = 10000L
    }
}
