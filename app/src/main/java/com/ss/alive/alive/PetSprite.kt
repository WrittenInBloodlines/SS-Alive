package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Small built-in pixel-style sprite renderer.
 * The frames are drawn independently so the prototype has a real animated
 * character without requiring an external image asset yet.
 */
object PetSprite {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }

    fun draw(canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        val scale = minOf(canvas.width, canvas.height) / 128f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(64f, 64f)
        if (direction < 0) canvas.scale(-1f, 1f)

        when (state) {
            PetBehavior.State.WALKING -> drawWalking(canvas, frame)
            PetBehavior.State.IDLE -> drawIdle(canvas, frame)
            PetBehavior.State.FALLING -> drawFalling(canvas, frame)
            PetBehavior.State.LANDING -> drawLanding(canvas, frame)
            PetBehavior.State.HELD -> drawHeld(canvas, frame)
        }

        canvas.restore()
    }

    private fun drawWalking(canvas: Canvas, frame: Int) {
        val bob = when (frame % 4) {
            0 -> 1f
            1 -> 0f
            2 -> -1f
            else -> 0f
        }
        val legOffset = when (frame % 4) {
            0 -> 5f
            2 -> -5f
            else -> 0f
        }
        drawCat(canvas, bob, legOffset, tailSwing = frame % 2 == 0)
    }

    private fun drawIdle(canvas: Canvas, frame: Int) {
        val blink = frame % 12 == 0 || frame % 12 == 1
        drawCat(canvas, 0f, 0f, tailSwing = frame % 8 < 2, blink = blink)
    }

    private fun drawFalling(canvas: Canvas, frame: Int) {
        canvas.save()
        canvas.rotate(-12f + (frame % 3) * 12f)
        drawCat(canvas, 0f, 0f, tailSwing = true)
        canvas.restore()
    }

    private fun drawLanding(canvas: Canvas, frame: Int) {
        val squash = when (frame % 4) {
            0 -> 1.00f
            1 -> 0.88f
            2 -> 0.94f
            else -> 1.00f
        }
        canvas.save()
        canvas.scale(1f, squash)
        drawCat(canvas, 7f, 0f, tailSwing = false)
        canvas.restore()
    }

    private fun drawHeld(canvas: Canvas, frame: Int) {
        val sway = if (frame % 2 == 0) -2f else 2f
        canvas.save()
        canvas.rotate(sway)
        drawCat(canvas, -1f, 0f, tailSwing = true)
        canvas.restore()
    }

    private fun drawCat(
        canvas: Canvas,
        bob: Float,
        legOffset: Float,
        tailSwing: Boolean,
        blink: Boolean = false
    ) {
        // Tail
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.strokeCap = Paint.Cap.ROUND
        val tailEnd = if (tailSwing) -10f else -18f
        canvas.drawLine(31f, 13f + bob, 42f, 4f + bob, paint)
        canvas.drawLine(42f, 4f + bob, 48f, tailEnd + bob, paint)

        // Body
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(-32f, -10f + bob, 30f, 35f + bob), 17f, 17f, paint)

        // Head
        canvas.drawRoundRect(RectF(-34f, -43f + bob, 32f, 15f + bob), 19f, 19f, paint)

        // Ears
        val leftEar = android.graphics.Path().apply {
            moveTo(-28f, -37f + bob)
            lineTo(-25f, -55f + bob)
            lineTo(-11f, -42f + bob)
            close()
        }
        val rightEar = android.graphics.Path().apply {
            moveTo(10f, -42f + bob)
            lineTo(25f, -55f + bob)
            lineTo(29f, -34f + bob)
            close()
        }
        canvas.drawPath(leftEar, paint)
        canvas.drawPath(rightEar, paint)

        // Face
        paint.color = android.graphics.Color.WHITE
        if (blink) {
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(-17f, -18f + bob, -8f, -18f + bob, paint)
            canvas.drawLine(8f, -18f + bob, 17f, -18f + bob, paint)
            paint.style = Paint.Style.FILL
        } else {
            canvas.drawCircle(-13f, -19f + bob, 4f, paint)
            canvas.drawCircle(13f, -19f + bob, 4f, paint)
        }
        paint.color = android.graphics.Color.BLACK
        canvas.drawCircle(-13f, -19f + bob, 2f, paint)
        canvas.drawCircle(13f, -19f + bob, 2f, paint)
        canvas.drawCircle(0f, -7f + bob, 3f, paint)

        // Legs
        paint.strokeWidth = 8f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        canvas.drawLine(-20f, 28f + bob, -20f + legOffset, 45f + bob, paint)
        canvas.drawLine(-3f, 30f + bob, -3f - legOffset, 46f + bob, paint)
        canvas.drawLine(13f, 29f + bob, 13f + legOffset, 45f + bob, paint)
        canvas.drawLine(26f, 25f + bob, 26f - legOffset, 42f + bob, paint)
        paint.style = Paint.Style.FILL
    }
}
