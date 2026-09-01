package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Built-in cute high-contrast Cat Template with state-specific frame counts. */
object PetSprite {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    fun frameCount(state: PetBehavior.State): Int = when (state) {
        PetBehavior.State.IDLE -> 8
        PetBehavior.State.WALKING -> 8
        PetBehavior.State.RUNNING -> 6
        PetBehavior.State.SIT -> 5
        PetBehavior.State.JUMP -> 6
        PetBehavior.State.FALLING -> 6
        PetBehavior.State.LANDING -> 5
        PetBehavior.State.HELD -> 6
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

    private fun drawIdle(canvas: Canvas, frame: Int) {
        val f = frame.mod(8)
        val bob = floatArrayOf(0f, -1f, -2f, -1f, 0f, 1f, 2f, 1f)[f]
        drawCat(canvas, bob, 0f, f, if (f == 3 || f == 4) 1 else 0, 1f)
    }

    private fun drawWalk(canvas: Canvas, frame: Int) {
        val f = frame.mod(8)
        val bob = floatArrayOf(1f, 0f, -1f, -2f, -1f, 0f, 1f, 0f)[f]
        val legs = floatArrayOf(7f, 3f, -5f, -7f, -3f, 5f, 7f, 3f)[f]
        drawCat(canvas, bob, legs, f, 0, 1f)
    }

    private fun drawRun(canvas: Canvas, frame: Int) {
        val f = frame.mod(6)
        val bob = floatArrayOf(2f, 0f, -2f, -1f, 1f, 2f)[f]
        val legs = floatArrayOf(11f, -8f, 9f, -11f, 8f, -9f)[f]
        drawCat(canvas, bob, legs, f, 0, 1.03f)
    }

    private fun drawSit(canvas: Canvas, frame: Int) {
        val f = frame.mod(5)
        drawCat(canvas, floatArrayOf(1f, 0f, -1f, 0f, 1f)[f], 0f, f + 2, if (f == 3) 1 else 0, 1f, true)
    }

    private fun drawJump(canvas: Canvas, frame: Int) {
        val f = frame.mod(6)
        val rotation = floatArrayOf(-7f, -4f, -1f, 2f, 5f, 7f)[f]
        val y = floatArrayOf(-2f, -5f, -8f, -8f, -5f, -2f)[f]
        canvas.save(); canvas.rotate(rotation)
        drawCat(canvas, y, floatArrayOf(9f, 5f, -7f, -9f, -5f, 7f)[f], f, 0, 1f)
        canvas.restore()
    }

    private fun drawFall(canvas: Canvas, frame: Int) {
        val f = frame.mod(6)
        canvas.save(); canvas.rotate(floatArrayOf(-8f, -18f, -29f, -40f, -51f, -62f)[f])
        drawCat(canvas, f.toFloat(), floatArrayOf(10f, -9f, 8f, -10f, 7f, -8f)[f], f + 1, 0, 1f)
        canvas.restore()
    }

    private fun drawLanding(canvas: Canvas, frame: Int) {
        val f = frame.mod(5)
        val squash = floatArrayOf(0.72f, 0.82f, 0.91f, 0.98f, 1f)[f]
        canvas.save(); canvas.scale(1f + (1f - squash) * 0.22f, squash)
        drawCat(canvas, floatArrayOf(8f, 6f, 4f, 2f, 1f)[f], 0f, f, 0, 1f)
        canvas.restore()
    }

    private fun drawHeld(canvas: Canvas, frame: Int) {
        val f = frame.mod(6)
        canvas.save()
        canvas.rotate(floatArrayOf(-4f, -2f, 0f, 3f, 4f, 1f)[f])
        drawCat(canvas, floatArrayOf(0f, 2f, 1f, 0f, -2f, -1f)[f], if (f % 2 == 0) 2f else -2f, f + 2, if (f == 3) 1 else 0, 0.98f)
        canvas.restore()
    }

    private fun drawCat(canvas: Canvas, bob: Float, legOffset: Float, tailPose: Int, eyePose: Int, bodyScale: Float, sitting: Boolean = false) {
        canvas.save(); canvas.scale(bodyScale, bodyScale)
        val body = RectF(-34f, -7f + bob, 31f, 35f + bob)
        fill.color = Color.WHITE; outline.color = Color.rgb(35, 35, 42); outline.strokeWidth = 6f
        canvas.drawRoundRect(body, 18f, 18f, outline); canvas.drawRoundRect(body, 18f, 18f, fill)

        // Black tail trails behind the body, so it appears on the left while the cat faces right.
        outline.color = Color.BLACK; outline.strokeWidth = 9f
        val tail = Path().apply {
            moveTo(27f, 12f + bob)
            when (tailPose.mod(4)) {
                0 -> cubicTo(44f, 6f + bob, 51f, -8f + bob, 43f, -18f + bob)
                1 -> cubicTo(47f, 12f + bob, 53f, 0f + bob, 44f, -5f + bob)
                2 -> cubicTo(46f, 26f + bob, 51f, 38f + bob, 38f, 42f + bob)
                else -> cubicTo(45f, 4f + bob, 55f, 18f + bob, 43f, 28f + bob)
            }
        }
        canvas.drawPath(tail, outline)

        // Head stays comfortably inside the sprite canvas, preventing the old clipping problem.
        val head = RectF(-35f, -53f + bob, 34f, 2f + bob)
        fill.color = Color.WHITE; outline.color = Color.rgb(35, 35, 42); outline.strokeWidth = 6f
        canvas.drawRoundRect(head, 20f, 20f, outline); canvas.drawRoundRect(head, 20f, 20f, fill)

        drawEar(canvas, -28f, -43f + bob, -25f, -61f + bob, -9f, -46f + bob)
        drawEar(canvas, 9f, -46f + bob, 25f, -61f + bob, 29f, -42f + bob)

        fill.color = Color.rgb(205, 205, 214)
        val leftInner = Path().apply { moveTo(-24f, -46f + bob); lineTo(-23f, -56f + bob); lineTo(-15f, -47f + bob); close() }
        val rightInner = Path().apply { moveTo(15f, -47f + bob); lineTo(24f, -56f + bob); lineTo(26f, -44f + bob); close() }
        canvas.drawPath(leftInner, fill); canvas.drawPath(rightInner, fill)

        fill.color = Color.BLACK
        if (eyePose == 1) {
            outline.color = Color.BLACK; outline.strokeWidth = 3f
            canvas.drawLine(-18f, -22f + bob, -9f, -22f + bob, outline)
            canvas.drawLine(9f, -22f + bob, 18f, -22f + bob, outline)
        } else {
            canvas.drawOval(RectF(-20f, -30f + bob, -8f, -14f + bob), fill)
            canvas.drawOval(RectF(8f, -30f + bob, 20f, -14f + bob), fill)
            fill.color = Color.WHITE
            canvas.drawCircle(-16f, -26f + bob, 2.5f, fill); canvas.drawCircle(13f, -26f + bob, 2.5f, fill)
        }

        fill.color = Color.rgb(45, 45, 52)
        canvas.drawCircle(0f, -7f + bob, 2.7f, fill)
        outline.color = Color.rgb(45, 45, 52); outline.strokeWidth = 1.8f
        canvas.drawLine(0f, -4f + bob, -5f, 0f + bob, outline); canvas.drawLine(0f, -4f + bob, 5f, 0f + bob, outline)

        fill.color = Color.rgb(60, 60, 68)
        canvas.drawRoundRect(RectF(-23f, 0f + bob, 22f, 6f + bob), 3f, 3f, fill)
        fill.color = Color.WHITE; canvas.drawCircle(0f, 7f + bob, 3f, fill)

        outline.color = Color.rgb(35, 35, 42); outline.strokeWidth = 7f; outline.strokeCap = Paint.Cap.ROUND
        val base = floatArrayOf(-22f, -7f, 9f, 24f)
        val legY = if (sitting) 42f else 46f
        for (i in 0..3) {
            val offset = if (sitting) 0f else legOffset * if (i % 2 == 0) 1f else -1f
            canvas.drawLine(base[i], 27f + bob, base[i] + offset, legY + bob, outline)
        }
        canvas.restore()
    }

    private fun drawEar(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        val ear = Path().apply { moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close() }
        fill.color = Color.WHITE; outline.color = Color.rgb(35, 35, 42); outline.strokeWidth = 5f
        canvas.drawPath(ear, outline); canvas.drawPath(ear, fill)
    }
}
