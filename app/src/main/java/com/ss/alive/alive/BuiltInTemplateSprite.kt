package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.sin

object BuiltInTemplateSprite {

    fun frameCount(kind: String, state: PetBehavior.State): Int {
        return if (kind == "CAT" || kind == "CUSTOM") {
            PetSprite.frameCount(state)
        } else {
            when (state) {
                PetBehavior.State.IDLE -> 10
                PetBehavior.State.WALKING -> 12
                PetBehavior.State.CLIMBING -> 10
                else -> 8
            }
        }
    }

    fun draw(
        kind: String,
        canvas: Canvas,
        frame: Int,
        state: PetBehavior.State,
        direction: Int
    ) {
        when (kind) {
            "DOG" -> drawDog(canvas, frame, direction)
            "CHICK" -> drawChick(canvas, frame, direction)
            else -> PetSprite.draw(canvas, frame, state, direction)
        }
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun setup(canvas: Canvas, direction: Int) {
        val scale = minOf(canvas.width, canvas.height) / 150f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(75f, 75f)

        if (direction < 0) {
            canvas.scale(-1f, 1f)
        }
    }

    private fun drawDog(
        canvas: Canvas,
        frame: Int,
        direction: Int
    ) {
        setup(canvas, direction)

        val legSwing = sin(frame * Math.PI / 6.0).toFloat() * 9f

        fill.color = Color.rgb(195, 145, 90)
        outline.color = Color.rgb(65, 45, 30)
        outline.strokeWidth = 5f

        val body = RectF(-38f, -8f, 30f, 35f)
        canvas.drawOval(body, fill)
        canvas.drawOval(body, outline)

        val head = RectF(12f, -47f, 55f, -7f)
        canvas.drawOval(head, fill)
        canvas.drawOval(head, outline)

        fill.color = Color.rgb(80, 55, 35)
        canvas.drawOval(RectF(16f, -55f, 29f, -23f), fill)
        canvas.drawOval(RectF(42f, -55f, 55f, -23f), fill)

        fill.color = Color.BLACK
        canvas.drawCircle(47f, -15f, 4f, fill)

        outline.color = Color.rgb(65, 45, 30)
        outline.strokeWidth = 6f

        for (index in 0..3) {
            val x = -25f + index * 17f
            val swing = if (index % 2 == 0) legSwing else -legSwing
            canvas.drawLine(x, 25f, x + swing, 48f, outline)
        }

        canvas.drawLine(
            -35f,
            4f,
            -52f,
            -5f + legSwing / 2f,
            outline
        )

        canvas.restore()
    }

    private fun drawChick(
        canvas: Canvas,
        frame: Int,
        direction: Int
    ) {
        setup(canvas, direction)

        val bounce = sin(frame * Math.PI / 5.0).toFloat() * 2f

        fill.color = Color.rgb(255, 220, 55)
        outline.color = Color.rgb(190, 150, 35)
        outline.strokeWidth = 4f

        canvas.drawCircle(-5f, 16f + bounce, 28f, fill)
        canvas.drawCircle(-5f, 16f + bounce, 28f, outline)

        canvas.drawCircle(17f, -15f + bounce, 23f, fill)
        canvas.drawCircle(17f, -15f + bounce, 23f, outline)

        fill.color = Color.BLACK
        canvas.drawCircle(25f, -20f + bounce, 3f, fill)

        fill.color = Color.rgb(245, 145, 35)
        val beak = Path().apply {
            moveTo(38f, -15f + bounce)
            lineTo(52f, -10f + bounce)
            lineTo(38f, -5f + bounce)
            close()
        }
        canvas.drawPath(beak, fill)

        outline.color = Color.rgb(220, 120, 25)
        outline.strokeWidth = 4f

        for (x in listOf(-10f, 5f)) {
            canvas.drawLine(
                x,
                40f + bounce,
                x + bounce * 3f,
                52f + bounce,
                outline
            )
        }

        canvas.restore()
    }
}
