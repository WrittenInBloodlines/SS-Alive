package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Built-in Cat Template sprite.
 *
 * Every supported state has four intentional animation poses. The sprite is
 * drawn locally so the Cat Template works without bundled image files.
 */
object PetSprite {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    fun draw(canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        val scale = minOf(canvas.width, canvas.height) / 128f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(64f, 64f)
        if (direction < 0) canvas.scale(-1f, 1f)

        when (state) {
            PetBehavior.State.WALKING -> drawWalk(canvas, frame)
            PetBehavior.State.RUNNING -> drawRun(canvas, frame)
            PetBehavior.State.IDLE -> drawIdle(canvas, frame)
            PetBehavior.State.SIT -> drawSit(canvas, frame)
            PetBehavior.State.JUMP -> drawJump(canvas, frame)
            PetBehavior.State.FALLING -> drawFall(canvas, frame)
            PetBehavior.State.LANDING -> drawLanding(canvas, frame)
            PetBehavior.State.HELD -> drawHeld(canvas, frame)
        }
        canvas.restore()
    }

    private fun drawWalk(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val bob = floatArrayOf(1f, 0f, -1f, 0f)[f]
        val legs = arrayOf(
            floatArrayOf(7f, -7f, -5f, 5f),
            floatArrayOf(3f, -3f, -2f, 2f),
            floatArrayOf(-7f, 7f, 5f, -5f),
            floatArrayOf(-3f, 3f, 2f, -2f)
        )[f]
        drawCat(canvas, bob, legs, tailPose = f, eyePose = 0, bodyScale = 1f)
    }

    private fun drawRun(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val bob = floatArrayOf(2f, -1f, -2f, -1f)[f]
        val legs = arrayOf(
            floatArrayOf(11f, -11f, -9f, 9f),
            floatArrayOf(7f, -7f, -5f, 5f),
            floatArrayOf(-11f, 11f, 9f, -9f),
            floatArrayOf(-7f, 7f, 5f, -5f)
        )[f]
        drawCat(canvas, bob, legs, tailPose = (f + 1) % 4, eyePose = 0, bodyScale = 1.04f)
    }

    private fun drawIdle(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val bob = floatArrayOf(0f, -1f, 0f, 1f)[f]
        val blink = f == 2
        drawCat(canvas, bob, floatArrayOf(0f, 0f, 0f, 0f), tailPose = f, eyePose = if (blink) 1 else 0, bodyScale = 1f)
    }

    private fun drawSit(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val bob = floatArrayOf(1f, 0f, -1f, 0f)[f]
        drawCat(canvas, bob + 5f, floatArrayOf(0f, 0f, 0f, 0f), tailPose = (f + 2) % 4, eyePose = 0, bodyScale = 1f, sitting = true)
    }

    private fun drawJump(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val rotations = floatArrayOf(-6f, -2f, 3f, 6f)
        val y = floatArrayOf(-3f, -7f, -7f, -3f)[f]
        canvas.save()
        canvas.rotate(rotations[f])
        drawCat(canvas, y, floatArrayOf(9f, -9f, 9f, -9f), tailPose = f, eyePose = 0, bodyScale = 1f)
        canvas.restore()
    }

    private fun drawFall(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val rotations = floatArrayOf(-10f, -25f, -40f, -55f)
        val y = floatArrayOf(-1f, 1f, 3f, 5f)[f]
        canvas.save()
        canvas.rotate(rotations[f])
        drawCat(canvas, y, floatArrayOf(10f, -10f, 10f, -10f), tailPose = (f + 1) % 4, eyePose = 0, bodyScale = 1f)
        canvas.restore()
    }

    private fun drawLanding(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val squash = floatArrayOf(0.72f, 0.84f, 0.94f, 1f)[f]
        val y = floatArrayOf(8f, 6f, 3f, 1f)[f]
        canvas.save()
        canvas.scale(1f + (1f - squash) * 0.18f, squash)
        drawCat(canvas, y, floatArrayOf(0f, 0f, 0f, 0f), tailPose = f, eyePose = 0, bodyScale = 1f)
        canvas.restore()
    }

    private fun drawHeld(canvas: Canvas, frame: Int) {
        val f = frame.mod(4)
        val rotations = floatArrayOf(-4f, -1f, 3f, 1f)
        val bob = floatArrayOf(0f, 2f, 0f, -2f)[f]
        canvas.save()
        canvas.rotate(rotations[f])
        drawCat(canvas, bob, floatArrayOf(2f, -2f, 2f, -2f), tailPose = (f + 2) % 4, eyePose = if (f == 3) 1 else 0, bodyScale = 0.98f)
        canvas.restore()
    }

    private fun drawCat(
        canvas: Canvas,
        bob: Float,
        legs: FloatArray,
        tailPose: Int,
        eyePose: Int,
        bodyScale: Float,
        sitting: Boolean = false
    ) {
        val bodyY = bob
        val headY = bob - 32f

        // Soft dark outline first, then the white sprite on top. This keeps the
        // character readable on both dark and light Android themes.
        outline.color = Color.rgb(35, 35, 42)
        outline.strokeWidth = 6f
        fill.color = Color.WHITE

        canvas.save()
        canvas.scale(bodyScale, bodyScale)

        val body = RectF(-34f, -7f + bodyY, 31f, 35f + bodyY)
        canvas.drawRoundRect(body, 18f, 18f, outline)
        canvas.drawRoundRect(body, 18f, 18f, fill)

        val head = RectF(-35f, -58f + headY, 34f, -3f + headY)
        canvas.drawRoundRect(head, 20f, 20f, outline)
        canvas.drawRoundRect(head, 20f, 20f, fill)

        drawEar(canvas, -27f, -47f + headY, -42f, -66f + headY, -9f, -50f + headY)
        drawEar(canvas, 10f, -50f + headY, 26f, -66f + headY, 29f, -43f + headY)

        // Inner ears.
        fill.color = Color.rgb(210, 210, 216)
        val leftInner = Path().apply {
            moveTo(-24f, -49f + headY)
            lineTo(-22f, -60f + headY)
            lineTo(-13f, -51f + headY)
            close()
        }
        val rightInner = Path().apply {
            moveTo(15f, -51f + headY)
            lineTo(25f, -60f + headY)
            lineTo(27f, -47f + headY)
            close()
        }
        canvas.drawPath(leftInner, fill)
        canvas.drawPath(rightInner, fill)

        // Tail has four deliberately different poses.
        outline.color = Color.WHITE
        outline.strokeWidth = 10f
        outline.style = Paint.Style.STROKE
        val tail = Path().apply {
            moveTo(27f, 12f + bodyY)
            when (tailPose.mod(4)) {
                0 -> { cubicTo(46f, 5f + bodyY, 48f, -10f + bodyY, 39f, -18f + bodyY) }
                1 -> { cubicTo(48f, 15f + bodyY, 53f, 1f + bodyY, 43f, -5f + bodyY) }
                2 -> { cubicTo(47f, 28f + bodyY, 49f, 39f + bodyY, 37f, 42f + bodyY) }
                else -> { cubicTo(45f, 4f + bodyY, 55f, 18f + bodyY, 43f, 27f + bodyY) }
            }
        }
        canvas.drawPath(tail, outline)
        outline.color = Color.rgb(35, 35, 42)
        outline.strokeWidth = 2.5f
        canvas.drawPath(tail, outline)

        // Eyes: large black eyes with tiny white highlights.
        fill.color = Color.BLACK
        if (eyePose == 1) {
            outline.color = Color.BLACK
            outline.strokeWidth = 3f
            canvas.drawLine(-18f, -27f + headY, -8f, -27f + headY, outline)
            canvas.drawLine(8f, -27f + headY, 18f, -27f + headY, outline)
        } else {
            canvas.drawOval(RectF(-20f, -34f + headY, -9f, -18f + headY), fill)
            canvas.drawOval(RectF(9f, -34f + headY, 20f, -18f + headY), fill)
            fill.color = Color.WHITE
            canvas.drawCircle(-16f, -30f + headY, 2.3f, fill)
            canvas.drawCircle(13f, -30f + headY, 2.3f, fill)
        }

        // Nose and tiny mouth.
        fill.color = Color.rgb(55, 55, 62)
        canvas.drawCircle(0f, -11f + headY, 3f, fill)
        outline.color = Color.rgb(55, 55, 62)
        outline.strokeWidth = 2f
        canvas.drawLine(0f, -8f + headY, -5f, -4f + headY, outline)
        canvas.drawLine(0f, -8f + headY, 5f, -4f + headY, outline)

        // Small collar detail.
        fill.color = Color.rgb(65, 65, 72)
        canvas.drawRoundRect(RectF(-23f, 0f + bodyY, 22f, 6f + bodyY), 3f, 3f, fill)
        fill.color = Color.WHITE
        canvas.drawCircle(0f, 7f + bodyY, 3f, fill)

        // Legs and paws, with four different walk poses.
        outline.color = Color.rgb(35, 35, 42)
        outline.strokeWidth = 7f
        outline.strokeCap = Paint.Cap.ROUND
        val legY = if (sitting) 43f else 46f
        val base = floatArrayOf(-22f, -7f, 9f, 24f)
        for (i in 0..3) {
            val x = base[i]
            val offset = if (sitting) 0f else legs[i]
            canvas.drawLine(x, 28f + bodyY, x + offset, legY + bodyY, outline)
        }

        canvas.restore()
    }

    private fun drawEar(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val ear = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
            lineTo(x3, y3)
            close()
        }
        outline.color = Color.rgb(35, 35, 42)
        outline.style = Paint.Style.STROKE
        outline.strokeWidth = 5f
        canvas.drawPath(ear, outline)
        fill.color = Color.WHITE
        canvas.drawPath(ear, fill)
    }
}
