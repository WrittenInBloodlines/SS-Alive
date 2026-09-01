package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hand-drawn built-in prototype sprites for the S•S Alive character templates.
 * These are intentionally detailed vector characters, not placeholder geometry.
 */
object BuiltInTemplateSprite {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun frameCount(kind: String, state: PetBehavior.State): Int {
        if (kind != "ALEX" && kind != "CIRO") return 1
        return when (state) {
            PetBehavior.State.IDLE -> 12
            PetBehavior.State.WALKING -> 16
            PetBehavior.State.RUNNING -> 16
            PetBehavior.State.CLIMBING -> 10
            PetBehavior.State.SIT -> 10
            PetBehavior.State.JUMP -> 10
            PetBehavior.State.FALLING -> 12
            PetBehavior.State.LANDING -> 10
            PetBehavior.State.HELD -> 10
        }
    }

    fun draw(kind: String, canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        if (kind != "ALEX" && kind != "CIRO") return
        val scale = minOf(canvas.width, canvas.height) / 210f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(105f, 104f)
        if (direction < 0 && state != PetBehavior.State.CLIMBING) canvas.scale(-1f, 1f)

        when (state) {
            PetBehavior.State.IDLE -> drawCharacter(canvas, kind, frame, state, 0f, 0f, false, 0f)
            PetBehavior.State.WALKING -> drawCharacter(canvas, kind, frame, state, walkBob(frame), walkSway(frame), false, 0f)
            PetBehavior.State.RUNNING -> drawCharacter(canvas, kind, frame, state, runBob(frame), runLean(frame), false, 0f)
            PetBehavior.State.SIT -> drawCharacter(canvas, kind, frame, state, 0f, 0f, true, 0f)
            PetBehavior.State.JUMP -> drawCharacter(canvas, kind, frame, state, jumpY(frame), jumpLean(frame), false, 0f)
            PetBehavior.State.FALLING -> drawCharacter(canvas, kind, frame, state, fallY(frame), fallLean(frame), false, fallRotation(frame))
            PetBehavior.State.LANDING -> drawCharacter(canvas, kind, frame, state, landingY(frame), 0f, false, 0f, landingSquash(frame))
            PetBehavior.State.HELD -> drawHeld(canvas, kind, frame)
            PetBehavior.State.CLIMBING -> drawCharacter(canvas, kind, frame, state, 0f, 0f, false, 0f)
        }
        canvas.restore()
    }

    private fun drawCharacter(
        c: Canvas,
        kind: String,
        frame: Int,
        state: PetBehavior.State,
        bob: Float,
        lean: Float,
        sitting: Boolean,
        rotation: Float,
        squash: Float = 1f
    ) {
        c.save()
        c.rotate(rotation, 0f, 36f)
        c.translate(0f, bob)
        c.scale(1f / squash, squash)
        c.rotate(lean, 0f, 35f)

        val running = state == PetBehavior.State.RUNNING
        val walking = state == PetBehavior.State.WALKING
        val gait = if (running) runPhase(frame) else if (walking) walkPhase(frame) else 0f
        val armSwing = if (running) gait * 0.45f else if (walking) gait * 0.25f else 0f
        val legSwing = if (running) gait * 1.35f else if (walking) gait else 0f
        val hairMotion = if (running) -gait * 0.22f else if (walking) -gait * 0.10f else 0f

        if (kind == "ALEX") drawAlex(c, frame, state, sitting, legSwing, armSwing, hairMotion)
        else drawCiro(c, frame, state, sitting, legSwing, armSwing, hairMotion)
        c.restore()
    }

    private fun drawAlex(c: Canvas, frame: Int, state: PetBehavior.State, sitting: Boolean, legSwing: Float, armSwing: Float, hairMotion: Float) {
        val skin = Color.rgb(247, 244, 239)
        val skinShadow = Color.rgb(224, 220, 218)
        val black = Color.rgb(18, 18, 22)
        val coat = Color.rgb(12, 12, 15)
        val coatLight = Color.rgb(35, 35, 40)
        val red = Color.rgb(185, 28, 43)
        val silver = Color.rgb(205, 205, 214)

        drawBackHair(c, black, hairMotion, longHair = true)
        drawLegs(c, black, coat, legSwing, sitting, tall = true)
        drawCoatBody(c, coat, coatLight, 0f, sitting, long = true)
        drawArms(c, skin, black, coat, armSwing, sitting, alex = true)
        drawNeckAndHead(c, skin, skinShadow, black, red, frame, hairMotion, alex = true)
        drawAlexDetails(c, silver)
    }

    private fun drawCiro(c: Canvas, frame: Int, state: PetBehavior.State, sitting: Boolean, legSwing: Float, armSwing: Float, hairMotion: Float) {
        val skin = Color.rgb(170, 125, 100)
        val skinLight = Color.rgb(190, 145, 118)
        val black = Color.rgb(25, 24, 28)
        val white = Color.rgb(238, 238, 235)
        val whiteShadow = Color.rgb(205, 205, 202)
        val brown = Color.rgb(83, 48, 35)
        val silver = Color.rgb(218, 218, 224)

        drawBackHair(c, black, hairMotion, longHair = false)
        drawLegs(c, black, white, legSwing, sitting, tall = false)
        drawCiroCoatBody(c, white, whiteShadow, sitting)
        drawArms(c, skin, black, white, armSwing, sitting, alex = false)
        drawNeckAndHead(c, skin, skinLight, black, brown, frame, hairMotion, alex = false)
        drawCiroDetails(c, silver)
    }

    private fun drawBackHair(c: Canvas, color: Int, motion: Float, longHair: Boolean) {
        fill.color = color
        val p = Path().apply {
            moveTo(-31f, -67f)
            cubicTo(-49f, -65f, -53f, -43f, -49f, -19f)
            if (longHair) {
                cubicTo(-56f, 4f, -55f, 46f + motion, -42f, 69f + motion)
                cubicTo(-35f, 52f, -29f, 31f, -26f, 5f)
            } else {
                cubicTo(-53f, -2f, -49f, 17f + motion, -35f, 27f + motion)
                cubicTo(-28f, 16f, -24f, 1f, -23f, -16f)
            }
            close()
        }
        c.drawPath(p, fill)
        line.color = Color.rgb(9, 9, 11)
        line.strokeWidth = 2.4f
        if (longHair) {
            for (i in 0..5) {
                val x = -45f + i * 4f
                c.drawLine(x, -54f, x - 2f + motion * 0.12f, 48f + i * 3f, line)
            }
        } else {
            for (i in 0..4) {
                val x = -43f + i * 5f
                c.drawArc(RectF(x - 7f, -63f, x + 8f, -45f), 20f, 190f, false, line)
            }
        }
    }

    private fun drawLegs(c: Canvas, shoe: Int, lower: Int, swing: Float, sitting: Boolean, tall: Boolean) {
        val y = if (sitting) 62f else 45f
        val spread = if (tall) 15f else 14f
        val s1 = swing
        val s2 = -swing
        drawLeg(c, -spread, y, s1, lower, shoe, tall)
        drawLeg(c, spread, y, s2, lower, shoe, tall)
    }

    private fun drawLeg(c: Canvas, x: Float, y: Float, swing: Float, lower: Int, shoe: Int, tall: Boolean) {
        val kneeX = x + swing * 0.35f
        val kneeY = y + 27f - kotlin.math.abs(swing) * 0.10f
        val footX = x + swing
        val footY = if (tall) 92f else 88f
        line.color = Color.rgb(8, 8, 10)
        line.strokeWidth = 11f
        c.drawLine(x, y, kneeX, kneeY, line)
        c.drawLine(kneeX, kneeY, footX, footY, line)
        line.color = lower
        line.strokeWidth = 7.5f
        c.drawLine(x, y, kneeX, kneeY, line)
        c.drawLine(kneeX, kneeY, footX, footY, line)
        fill.color = shoe
        c.drawOval(RectF(footX - 9f, footY - 3f, footX + 10f, footY + 7f), fill)
    }

    private fun drawCoatBody(c: Canvas, coat: Int, light: Int, shift: Float, sitting: Boolean, long: Boolean) {
        val bottom = if (sitting) 65f else if (long) 79f else 68f
        val p = Path().apply {
            moveTo(-30f, -3f)
            cubicTo(-39f, 16f, -35f, 42f, -31f, bottom)
            lineTo(31f, bottom)
            cubicTo(35f, 40f, 38f, 16f, 30f, -3f)
            close()
        }
        fill.color = coat
        c.drawPath(p, fill)
        line.color = Color.rgb(3, 3, 4)
        line.strokeWidth = 3f
        c.drawPath(p, line)
        fill.color = light
        val lapel = Path().apply {
            moveTo(-24f, -2f); lineTo(-5f, 5f); lineTo(-1f, 45f); lineTo(-17f, 25f); close()
        }
        c.drawPath(lapel, fill)
        val lapel2 = Path().apply {
            moveTo(24f, -2f); lineTo(5f, 5f); lineTo(1f, 45f); lineTo(17f, 25f); close()
        }
        c.drawPath(lapel2, fill)
        line.color = Color.rgb(60, 60, 67)
        line.strokeWidth = 1.6f
        c.drawLine(0f, 5f, 0f, bottom - 3f, line)
        for (i in 0..3) c.drawCircle(0f, 16f + i * 12f, 1.7f, fill)
    }

    private fun drawCiroCoatBody(c: Canvas, white: Int, shadow: Int, sitting: Boolean) {
        val bottom = if (sitting) 63f else 68f
        val p = Path().apply {
            moveTo(-29f, -2f)
            cubicTo(-36f, 18f, -34f, 43f, -28f, bottom)
            lineTo(28f, bottom)
            cubicTo(34f, 43f, 36f, 18f, 29f, -2f)
            close()
        }
        fill.color = white
        c.drawPath(p, fill)
        line.color = Color.rgb(32, 32, 36)
        line.strokeWidth = 3f
        c.drawPath(p, line)
        fill.color = shadow
        val left = Path().apply { moveTo(-23f, -1f); lineTo(-4f, 6f); lineTo(-1f, 43f); lineTo(-17f, 25f); close() }
        val right = Path().apply { moveTo(23f, -1f); lineTo(4f, 6f); lineTo(1f, 43f); lineTo(17f, 25f); close() }
        c.drawPath(left, fill)
        c.drawPath(right, fill)
        line.color = Color.rgb(70, 70, 74)
        line.strokeWidth = 1.6f
        c.drawLine(0f, 6f, 0f, bottom - 2f, line)
        for (i in 0..3) {
            fill.color = Color.rgb(70, 70, 74)
            c.drawCircle(0f, 16f + i * 12f, 1.7f, fill)
        }
    }

    private fun drawArms(c: Canvas, skin: Int, glove: Int, coat: Int, swing: Float, sitting: Boolean, alex: Boolean) {
        val shoulderY = 1f
        val length = if (sitting) 31f else 39f
        val leftAngle = -16f + swing
        val rightAngle = 16f - swing
        drawArm(c, -27f, shoulderY, leftAngle, length, coat, glove, alex)
        drawArm(c, 27f, shoulderY, rightAngle, length, coat, glove, alex)
    }

    private fun drawArm(c: Canvas, x: Float, y: Float, angle: Float, length: Float, coat: Int, glove: Int, alex: Boolean) {
        val rad = Math.toRadians(angle.toDouble())
        val elbowX = x + cos(rad).toFloat() * length * 0.48f * if (x < 0f) -1f else 1f
        val elbowY = y + sin(rad).toFloat() * length * 0.48f + 8f
        val handX = x + cos(rad).toFloat() * length * if (x < 0f) -1f else 1f
        val handY = y + sin(rad).toFloat() * length + 10f
        line.color = Color.rgb(6, 6, 8)
        line.strokeWidth = 10f
        c.drawLine(x, y, elbowX, elbowY, line)
        c.drawLine(elbowX, elbowY, handX, handY, line)
        line.color = coat
        line.strokeWidth = 7f
        c.drawLine(x, y, elbowX, elbowY, line)
        c.drawLine(elbowX, elbowY, handX, handY, line)
        fill.color = glove
        c.drawOval(RectF(handX - 5f, handY - 4f, handX + 5f, handY + 7f), fill)
        if (!alex) {
            line.color = Color.rgb(95, 65, 50)
            line.strokeWidth = 1.4f
            c.drawLine(handX, handY, handX + 3f, handY + 2f, line)
        }
    }

    private fun drawNeckAndHead(c: Canvas, skin: Int, skinLight: Int, hair: Int, eye: Int, frame: Int, hairMotion: Float, alex: Boolean) {
        fill.color = skin
        c.drawRoundRect(RectF(-9f, -12f, 9f, 7f), 6f, 6f, fill)

        val face = Path().apply {
            moveTo(-29f, -58f)
            cubicTo(-27f, -76f, -14f, -87f, 0f, -87f)
            cubicTo(17f, -87f, 29f, -75f, 29f, -57f)
            cubicTo(28f, -37f, 17f, -22f, 0f, -20f)
            cubicTo(-18f, -22f, -28f, -37f, -29f, -58f)
            close()
        }
        fill.color = skin
        c.drawPath(face, fill)
        line.color = Color.rgb(40, 34, 34)
        line.strokeWidth = 2.1f
        c.drawPath(face, line)

        fill.color = hair
        if (alex) {
            val fringe = Path().apply {
                moveTo(-30f, -61f)
                cubicTo(-28f, -82f, -17f, -93f, 0f, -94f)
                cubicTo(18f, -93f, 31f, -82f, 30f, -62f)
                lineTo(18f, -69f)
                cubicTo(10f, -61f, 4f, -75f, -3f, -67f)
                cubicTo(-10f, -59f, -17f, -72f, -22f, -62f)
                close()
            }
            c.drawPath(fringe, fill)
            line.color = Color.rgb(8, 8, 10)
            line.strokeWidth = 2f
            for (i in 0..5) {
                val x = -24f + i * 9f
                c.drawLine(x, -82f, x + hairMotion * 0.12f, -52f, line)
            }
        } else {
            val curls = arrayOf(-24f, -15f, -5f, 6f, 17f, 25f)
            curls.forEachIndexed { i, x ->
                val y = -77f + (i % 2) * 7f
                c.drawCircle(x, y, 9f, fill)
                c.drawCircle(x + 4f, y + 8f + hairMotion * 0.15f, 7f, fill)
            }
        }

        drawEyes(c, eye, frame, alex)
        fill.color = if (alex) Color.rgb(155, 35, 46) else Color.rgb(65, 39, 26)
        val nose = Path().apply { moveTo(1f, -51f); lineTo(-2f, -39f); lineTo(3f, -38f); close() }
        c.drawPath(nose, fill)
        line.color = Color.rgb(75, 45, 40)
        line.strokeWidth = 1.6f
        c.drawLine(-7f, -28f, 7f, -28f, line)
        if (!alex) {
            fill.color = Color.rgb(35, 25, 23)
            c.drawOval(RectF(-12f, -35f, 12f, -25f), fill)
            fill.color = skin
            c.drawOval(RectF(-12f, -36f, 12f, -30f), fill)
        }
    }

    private fun drawEyes(c: Canvas, eyeColor: Int, frame: Int, alex: Boolean) {
        val blink = frame % 12 == 5 || frame % 12 == 6
        if (blink) {
            line.color = Color.rgb(45, 35, 35)
            line.strokeWidth = 2.3f
            c.drawLine(-19f, -53f, -8f, -52f, line)
            c.drawLine(8f, -52f, 19f, -53f, line)
            return
        }
        line.color = Color.rgb(38, 30, 32)
        line.strokeWidth = 2f
        c.drawArc(RectF(-22f, -60f, -7f, -47f), 190f, 160f, false, line)
        c.drawArc(RectF(7f, -60f, 22f, -47f), 190f, 160f, false, line)
        fill.color = Color.WHITE
        c.drawOval(RectF(-20f, -57f, -8f, -48f), fill)
        c.drawOval(RectF(8f, -57f, 20f, -48f), fill)
        fill.color = eyeColor
        c.drawCircle(-14f, -52f, 4.2f, fill)
        c.drawCircle(14f, -52f, 4.2f, fill)
        fill.color = Color.BLACK
        c.drawCircle(-14f, -52f, 1.9f, fill)
        c.drawCircle(14f, -52f, 1.9f, fill)
        fill.color = Color.WHITE
        c.drawCircle(-15f, -53f, 0.9f, fill)
        c.drawCircle(13f, -53f, 0.9f, fill)
    }

    private fun drawAlexDetails(c: Canvas, silver: Int) {
        fill.color = silver
        c.drawCircle(17f, -39f, 3.3f, fill)
        line.color = Color.rgb(190, 190, 198)
        line.strokeWidth = 2f
        c.drawCircle(7f, 4f, 4.2f, line)
        c.drawCircle(7f, 4f, 2.8f, line)
    }

    private fun drawCiroDetails(c: Canvas, silver: Int) {
        fill.color = Color.WHITE
        c.drawCircle(24f, -43f, 3.4f, fill)
        line.color = silver
        line.strokeWidth = 1.5f
        c.drawCircle(24f, -43f, 3.8f, line)
        line.color = Color.rgb(150, 150, 156)
        line.strokeWidth = 2f
        c.drawCircle(7f, 4f, 4.2f, line)
        c.drawCircle(7f, 4f, 2.8f, line)
    }

    private fun drawHeld(c: Canvas, kind: String, frame: Int) {
        val sway = sin(frame * Math.PI / 5.0).toFloat() * 2.2f
        c.save()
        c.rotate(sway)
        drawCharacter(c, kind, frame, PetBehavior.State.HELD, heldBob(frame), 0f, false, 0f)
        c.restore()
    }

    private fun walkPhase(frame: Int): Float = sin(frame * Math.PI * 2.0 / 16.0).toFloat()
    private fun runPhase(frame: Int): Float = sin(frame * Math.PI * 2.0 / 16.0).toFloat()
    private fun walkBob(frame: Int): Float = absWave(frame, 16) * 1.3f
    private fun runBob(frame: Int): Float = absWave(frame, 16) * 2.0f
    private fun walkSway(frame: Int): Float = sin(frame * Math.PI * 2.0 / 16.0).toFloat() * 0.7f
    private fun runLean(frame: Int): Float = -2.0f + sin(frame * Math.PI * 2.0 / 16.0).toFloat() * 0.5f
    private fun jumpY(frame: Int): Float = floatArrayOf(0f, -3f, -8f, -14f, -19f, -21f, -18f, -12f, -6f, -1f)[frame % 10]
    private fun jumpLean(frame: Int): Float = floatArrayOf(0f, -1f, -2f, -2f, -1f, 0f, 1f, 1f, 0f, 0f)[frame % 10]
    private fun fallY(frame: Int): Float = floatArrayOf(-5f, -2f, 2f, 7f, 13f, 19f, 24f, 28f, 31f, 32f, 31f, 29f)[frame % 12]
    private fun fallLean(frame: Int): Float = sin(frame * Math.PI / 6.0).toFloat() * 3f
    private fun fallRotation(frame: Int): Float = floatArrayOf(0f, -8f, -18f, -28f, -38f, -46f, -55f, -60f, -48f, -30f, -12f, 0f)[frame % 12]
    private fun landingY(frame: Int): Float = floatArrayOf(7f, 6f, 5f, 3f, 2f, 1f, 0f, 0f, 0f, 0f)[frame % 10]
    private fun landingSquash(frame: Int): Float = floatArrayOf(0.72f, 0.78f, 0.84f, 0.90f, 0.95f, 0.98f, 1f, 1f, 1f, 1f)[frame % 10]
    private fun heldBob(frame: Int): Float = sin(frame * Math.PI * 2.0 / 10.0).toFloat() * 1.2f
    private fun absWave(frame: Int, count: Int): Float = kotlin.math.abs(sin(frame * Math.PI * 2.0 / count).toFloat())
}
