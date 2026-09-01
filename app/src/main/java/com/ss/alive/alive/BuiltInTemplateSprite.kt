package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object BuiltInTemplateSprite {

    fun frameCount(kind: String, state: PetBehavior.State): Int {
        if (kind == "CAT" || kind == "CUSTOM") return PetSprite.frameCount(state)

        return when (kind) {
            "DOG" -> when (state) {
                PetBehavior.State.IDLE -> 12
                PetBehavior.State.WALKING -> 10
                PetBehavior.State.RUNNING -> 8
                PetBehavior.State.CLIMBING -> 8
                PetBehavior.State.SIT -> 10
                PetBehavior.State.FALLING -> 12
                PetBehavior.State.LANDING -> 6
                PetBehavior.State.JUMP -> 8
                PetBehavior.State.HELD -> 6
            }
            "CHICK" -> when (state) {
                PetBehavior.State.IDLE -> 10
                PetBehavior.State.WALKING -> 12
                PetBehavior.State.RUNNING -> 10
                PetBehavior.State.CLIMBING -> 10
                PetBehavior.State.SIT -> 8
                PetBehavior.State.FALLING -> 12
                PetBehavior.State.LANDING -> 6
                PetBehavior.State.JUMP -> 8
                PetBehavior.State.HELD -> 6
            }
            else -> 8
        }
    }

    fun draw(kind: String, canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        when (kind) {
            "DOG" -> drawDog(canvas, frame, state, direction)
            "CHICK" -> drawChick(canvas, frame, state, direction)
            else -> PetSprite.draw(canvas, frame, state, direction)
        }
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun setup(canvas: Canvas, direction: Int, state: PetBehavior.State, frame: Int) {
        val scale = minOf(canvas.width, canvas.height) / 170f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(85f, 85f)

        if (state == PetBehavior.State.FALLING) {
            canvas.rotate(frame * 30f)
        } else if (state == PetBehavior.State.LANDING) {
            canvas.scale(1.05f, 0.86f)
        } else if (state == PetBehavior.State.HELD) {
            canvas.rotate(sin(frame * Math.PI / 3.0).toFloat() * 3f)
        }

        if (direction < 0 && state != PetBehavior.State.CLIMBING) {
            canvas.scale(-1f, 1f)
        }
    }

    private fun finish(canvas: Canvas) = canvas.restore()

    private fun drawDog(canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        setup(canvas, direction, state, frame)

        val phase = frame.toDouble()
        val walk = if (state == PetBehavior.State.WALKING || state == PetBehavior.State.RUNNING) {
            sin(phase * Math.PI * 2.0 / frameCount("DOG", state)).toFloat()
        } else 0f
        val bounce = if (state == PetBehavior.State.WALKING || state == PetBehavior.State.RUNNING) absLike(walk) * 2.4f else 0f
        val earFlop = sin(phase * Math.PI / 4.0).toFloat() * 2.2f
        val tailWag = when (state) {
            PetBehavior.State.IDLE -> sin(phase * Math.PI / 5.0).toFloat() * 5f
            PetBehavior.State.SIT -> sin(phase * Math.PI / 4.0).toFloat() * 8f
            else -> walk * 5f
        }

        fill.color = Color.rgb(191, 139, 87)
        outline.color = Color.rgb(70, 48, 30)
        outline.strokeWidth = 4.5f

        val body = RectF(-48f, -12f - bounce, 26f, 35f - bounce)
        canvas.drawOval(body, fill)
        canvas.drawOval(body, outline)

        val chest = RectF(8f, -2f - bounce, 36f, 31f - bounce)
        fill.color = Color.rgb(235, 215, 185)
        canvas.drawOval(chest, fill)

        fill.color = Color.rgb(191, 139, 87)
        val head = RectF(13f, -54f - bounce, 62f, -8f - bounce)
        canvas.drawOval(head, fill)
        canvas.drawOval(head, outline)

        fill.color = Color.rgb(92, 58, 38)
        val ear1 = Path().apply {
            moveTo(21f, -47f - bounce)
            lineTo(13f, -68f - bounce + earFlop)
            lineTo(36f, -53f - bounce)
            close()
        }
        val ear2 = Path().apply {
            moveTo(47f, -48f - bounce)
            lineTo(57f, -67f - bounce - earFlop)
            lineTo(63f, -45f - bounce)
            close()
        }
        canvas.drawPath(ear1, fill)
        canvas.drawPath(ear1, outline)
        canvas.drawPath(ear2, fill)
        canvas.drawPath(ear2, outline)

        fill.color = Color.rgb(48, 34, 26)
        canvas.drawCircle(49f, -24f - bounce, 4.5f, fill)
        canvas.drawCircle(33f, -30f - bounce, 3.2f, fill)

        fill.color = Color.rgb(240, 220, 198)
        canvas.drawOval(RectF(43f, -20f - bounce, 65f, -4f - bounce), fill)
        fill.color = Color.rgb(38, 28, 24)
        canvas.drawOval(RectF(57f, -16f - bounce, 64f, -10f - bounce), fill)

        outline.strokeWidth = 5.5f
        canvas.drawLine(-43f, 2f - bounce, -67f, -13f - bounce + tailWag, outline)
        canvas.drawLine(-67f, -13f - bounce + tailWag, -77f, -1f - bounce + tailWag * 1.3f, outline)

        if (state == PetBehavior.State.SIT) {
            drawDogLeg(canvas, -20f, 28f, 0f, bounce)
            drawDogLeg(canvas, 7f, 28f, 0f, bounce)
        } else {
            val legA = walk * 10f
            val legB = -walk * 10f
            drawDogLeg(canvas, -31f, 27f - bounce, legA, 0f)
            drawDogLeg(canvas, -10f, 30f - bounce, legB, 0f)
            drawDogLeg(canvas, 8f, 29f - bounce, legB, 0f)
            drawDogLeg(canvas, 25f, 24f - bounce, legA, 0f)
        }

        if (state == PetBehavior.State.CLIMBING) {
            outline.strokeWidth = 4f
            canvas.drawLine(-42f, 8f, -67f, -10f, outline)
            canvas.drawLine(25f, 25f, 44f, 44f, outline)
        }

        finish(canvas)
    }

    private fun drawDogLeg(canvas: Canvas, x: Float, y: Float, swing: Float, bounce: Float) {
        outline.color = Color.rgb(70, 48, 30)
        outline.strokeWidth = 7f
        canvas.drawLine(x, y, x + swing * 0.45f, y + 16f + bounce, outline)
        canvas.drawLine(x + swing * 0.45f, y + 16f + bounce, x + swing, y + 27f + bounce, outline)
    }

    private fun drawChick(canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        setup(canvas, direction, state, frame)

        val count = frameCount("CHICK", state)
        val phase = frame.toDouble()
        val step = sin(phase * Math.PI * 2.0 / count).toFloat()
        val bounce = if (state == PetBehavior.State.WALKING || state == PetBehavior.State.RUNNING) absLike(step) * 4f else 0f
        val wing = when (state) {
            PetBehavior.State.FALLING -> sin(phase * Math.PI * 1.8).toFloat() * 18f
            PetBehavior.State.IDLE -> sin(phase * Math.PI / 5.0).toFloat() * 3f
            PetBehavior.State.SIT -> sin(phase * Math.PI / 4.0).toFloat() * 5f
            else -> step * 8f
        }

        fill.color = Color.rgb(255, 214, 54)
        outline.color = Color.rgb(178, 137, 25)
        outline.strokeWidth = 4f

        canvas.drawCircle(-8f, 22f + bounce, 34f, fill)
        canvas.drawCircle(-8f, 22f + bounce, 34f, outline)

        val head = RectF(-1f, -43f + bounce, 53f, 9f + bounce)
        canvas.drawOval(head, fill)
        canvas.drawOval(head, outline)

        fill.color = Color.rgb(255, 232, 105)
        val wingPath = Path().apply {
            moveTo(-18f, 8f + bounce)
            quadTo(-53f, 10f + bounce + wing, -35f, 40f + bounce)
            quadTo(-5f, 30f + bounce, -18f, 8f + bounce)
            close()
        }
        canvas.drawPath(wingPath, fill)
        canvas.drawPath(wingPath, outline)

        val otherWing = Path().apply {
            moveTo(3f, 8f + bounce)
            quadTo(25f, 0f + bounce - wing * 0.4f, 28f, 32f + bounce)
            quadTo(6f, 34f + bounce, 3f, 8f + bounce)
            close()
        }
        canvas.drawPath(otherWing, fill)
        canvas.drawPath(otherWing, outline)

        fill.color = Color.rgb(42, 34, 22)
        canvas.drawOval(RectF(26f, -22f + bounce, 32f, -13f + bounce), fill)

        fill.color = Color.rgb(244, 141, 25)
        val beak = Path().apply {
            moveTo(48f, -10f + bounce)
            lineTo(66f, -3f + bounce)
            lineTo(48f, 5f + bounce)
            close()
        }
        canvas.drawPath(beak, fill)
        canvas.drawPath(beak, outline)

        fill.color = Color.rgb(244, 141, 25)
        canvas.drawLine(-15f, 53f + bounce, -15f + step * 6f, 66f + bounce, outline.apply { color = Color.rgb(205, 120, 20); strokeWidth = 4f })
        canvas.drawLine(8f, 53f + bounce, 8f - step * 6f, 66f + bounce, outline)
        canvas.drawLine(-21f + step * 6f, 66f + bounce, -8f + step * 6f, 66f + bounce, outline)
        canvas.drawLine(2f - step * 6f, 66f + bounce, 15f - step * 6f, 66f + bounce, outline)

        if (state == PetBehavior.State.IDLE && frame % count > count / 2) {
            outline.color = Color.rgb(42, 34, 22)
            outline.strokeWidth = 2.5f
            canvas.drawLine(25f, -17f + bounce, 33f, -17f + bounce, outline)
        }

        finish(canvas)
    }

    private fun absLike(value: Float): Float = if (value < 0f) -value else value
}
