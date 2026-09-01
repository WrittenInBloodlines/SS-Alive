package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hand-drawn vector characters for the first S•S Alive Alex/Ciro prototype.
 * The art is deliberately character-shaped rather than placeholder geometry.
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
            PetBehavior.State.RUNNING -> 18
            PetBehavior.State.CLIMBING -> 10
            PetBehavior.State.SIT -> 12
            PetBehavior.State.JUMP -> 12
            PetBehavior.State.FALLING -> 14
            PetBehavior.State.LANDING -> 12
            PetBehavior.State.HELD -> 12
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
            PetBehavior.State.WALKING -> drawCharacter(canvas, kind, frame, state, walkBob(frame), walkLean(frame), false, 0f)
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
        c.rotate(rotation, 0f, 35f)
        c.translate(0f, bob)
        c.scale(1f / squash, squash)
        c.rotate(lean, 0f, 35f)

        val running = state == PetBehavior.State.RUNNING
        val walking = state == PetBehavior.State.WALKING
        val phase = when {
            running -> runPhase(frame)
            walking -> walkPhase(frame)
            else -> 0f
        }
        val armSwing = when {
            running -> phase * 0.34f
            walking -> phase * 0.20f
            else -> 0f
        }
        val legSwing = when {
            running -> phase * 1.15f
            walking -> phase * 0.78f
            else -> 0f
        }
        val hairMotion = when {
            running -> -phase * 0.45f
            walking -> -phase * 0.16f
            else -> 0f
        }

        if (kind == "ALEX") drawAlex(c, frame, state, sitting, legSwing, armSwing, hairMotion)
        else drawCiro(c, frame, state, sitting, legSwing, armSwing, hairMotion)
        c.restore()
    }

    private fun drawAlex(c: Canvas, frame: Int, state: PetBehavior.State, sitting: Boolean, legSwing: Float, armSwing: Float, hairMotion: Float) {
        val skin = Color.rgb(239, 232, 224)
        val skinLight = Color.rgb(249, 243, 236)
        val skinShadow = Color.rgb(207, 197, 191)
        val hair = Color.rgb(246, 244, 241)
        val hairShadow = Color.rgb(207, 205, 203)
        val coat = Color.rgb(15, 15, 18)
        val coatLight = Color.rgb(38, 38, 44)
        val red = Color.rgb(177, 29, 45)
        val redDark = Color.rgb(105, 18, 30)
        val silver = Color.rgb(200, 200, 208)

        drawAlexBackHair(c, hair, hairShadow, hairMotion)
        drawLegs(c, coat, coat, legSwing, sitting, tall = true)
        drawAlexCoat(c, coat, coatLight, sitting)
        drawArms(c, skin, coat, coat, armSwing, sitting, alex = true)
        drawHead(c, skin, skinLight, skinShadow, hair, hairShadow, red, frame, hairMotion, alex = true)
        drawAlexFaceDetails(c, redDark)
        drawHands(c, skin, coat, armSwing, sitting, alex = true)
    }

    private fun drawCiro(c: Canvas, frame: Int, state: PetBehavior.State, sitting: Boolean, legSwing: Float, armSwing: Float, hairMotion: Float) {
        val skin = Color.rgb(156, 105, 80)
        val skinLight = Color.rgb(178, 125, 96)
        val skinShadow = Color.rgb(121, 77, 61)
        val hair = Color.rgb(28, 25, 29)
        val hairLight = Color.rgb(48, 42, 46)
        val white = Color.rgb(241, 240, 236)
        val whiteShadow = Color.rgb(208, 207, 203)
        val brown = Color.rgb(83, 49, 34)
        val brownDark = Color.rgb(48, 31, 25)
        val silver = Color.rgb(218, 218, 224)

        drawCiroBackHair(c, hair, hairLight, hairMotion)
        drawLegs(c, white, Color.rgb(29, 28, 31), legSwing, sitting, tall = false)
        drawCiroCoat(c, white, whiteShadow, sitting)
        drawArms(c, skin, hair, white, armSwing, sitting, alex = false)
        drawHead(c, skin, skinLight, skinShadow, hair, hairLight, brown, frame, hairMotion, alex = false)
        drawCiroFaceDetails(c, brownDark, silver)
        drawHands(c, skin, white, armSwing, sitting, alex = false)
    }

    private fun drawAlexBackHair(c: Canvas, hair: Int, shadow: Int, motion: Float) {
        fill.color = hair
        val p = Path().apply {
            moveTo(-28f, -69f)
            cubicTo(-43f, -64f, -48f, -47f, -46f, -25f)
            cubicTo(-48f, 2f, -50f, 35f + motion, -43f, 70f + motion)
            cubicTo(-38f, 79f + motion, -32f, 69f, -29f, 55f)
            cubicTo(-27f, 34f, -24f, 11f, -22f, -8f)
            close()
        }
        c.drawPath(p, fill)
        val p2 = Path().apply {
            moveTo(27f, -69f)
            cubicTo(42f, -64f, 48f, -45f, 46f, -24f)
            cubicTo(49f, 5f, 50f, 39f + motion, 42f, 72f + motion)
            cubicTo(37f, 79f + motion, 31f, 67f, 28f, 53f)
            cubicTo(26f, 31f, 24f, 9f, 22f, -8f)
            close()
        }
        c.drawPath(p2, fill)

        line.color = shadow
        line.strokeWidth = 1.6f
        for (i in 0..6) {
            val x = -42f + i * 5f
            c.drawLine(x, -56f, x - 2f + motion * 0.10f, 55f + i * 2f, line)
        }
        for (i in 0..6) {
            val x = 42f - i * 5f
            c.drawLine(x, -55f, x + 2f + motion * 0.10f, 57f + i * 2f, line)
        }
    }

    private fun drawCiroBackHair(c: Canvas, hair: Int, highlight: Int, motion: Float) {
        fill.color = hair
        val p = Path().apply {
            moveTo(-30f, -69f)
            cubicTo(-45f, -65f, -49f, -51f, -46f, -35f)
            cubicTo(-50f, -20f, -46f, 0f, -43f, 18f + motion)
            cubicTo(-41f, 32f, -35f, 38f + motion, -29f, 29f)
            cubicTo(-25f, 17f, -24f, 2f, -23f, -13f)
            close()
        }
        c.drawPath(p, fill)
        val right = Path().apply {
            moveTo(30f, -69f)
            cubicTo(45f, -65f, 49f, -51f, 46f, -35f)
            cubicTo(50f, -20f, 46f, 1f, 43f, 18f + motion)
            cubicTo(41f, 32f, 35f, 38f + motion, 29f, 29f)
            cubicTo(25f, 17f, 24f, 2f, 23f, -13f)
            close()
        }
        c.drawPath(right, fill)

        line.color = highlight
        line.strokeWidth = 2f
        val curlXs = floatArrayOf(-40f, -31f, -21f, 21f, 31f, 40f)
        curlXs.forEachIndexed { index, x ->
            val y = -48f + (index % 2) * 8f
            c.drawArc(RectF(x - 8f, y - 7f, x + 8f, y + 9f), 15f, 230f, false, line)
        }
    }

    private fun drawLegs(c: Canvas, upper: Int, lower: Int, swing: Float, sitting: Boolean, tall: Boolean) {
        val hipY = if (sitting) 51f else 39f
        val hipSpread = if (tall) 12f else 11f
        val upperLength = if (tall) 28f else 25f
        val lowerLength = if (tall) 31f else 29f
        drawLeg(c, -hipSpread, hipY, swing, upper, lower, upperLength, lowerLength, left = true, sitting = sitting)
        drawLeg(c, hipSpread, hipY, -swing, upper, lower, upperLength, lowerLength, left = false, sitting = sitting)
    }

    private fun drawLeg(c: Canvas, x: Float, y: Float, swing: Float, upper: Int, lower: Int, upperLength: Float, lowerLength: Float, left: Boolean, sitting: Boolean) {
        val angle = Math.toRadians((if (left) -90f else -90f + swing * 0.55f).toDouble())
        val kneeX = x + cos(angle).toFloat() * (upperLength * 0.22f) + swing * 0.25f
        val kneeY = y + upperLength
        val footX = kneeX + swing
        val footY = kneeY + lowerLength
        line.color = Color.rgb(8, 8, 10)
        line.strokeWidth = 13f
        c.drawLine(x, y, kneeX, kneeY, line)
        c.drawLine(kneeX, kneeY, footX, footY, line)
        line.color = upper
        line.strokeWidth = 9.5f
        c.drawLine(x, y, kneeX, kneeY, line)
        line.color = lower
        line.strokeWidth = 8.5f
        c.drawLine(kneeX, kneeY, footX, footY, line)
        fill.color = Color.rgb(18, 18, 21)
        val shoeLift = if (sitting) 1f else 0f
        c.drawRoundRect(RectF(footX - 10f, footY - 3f + shoeLift, footX + 11f, footY + 7f + shoeLift), 4f, 4f, fill)
    }

    private fun drawAlexCoat(c: Canvas, coat: Int, light: Int, sitting: Boolean) {
        val bottom = if (sitting) 66f else 78f
        val p = Path().apply {
            moveTo(-29f, -4f)
            cubicTo(-36f, 14f, -35f, 40f, -31f, bottom)
            cubicTo(-20f, bottom + 4f, -10f, bottom + 2f, 0f, bottom + 3f)
            cubicTo(10f, bottom + 2f, 20f, bottom + 4f, 31f, bottom)
            cubicTo(35f, 40f, 36f, 14f, 29f, -4f)
            close()
        }
        fill.color = coat
        c.drawPath(p, fill)
        line.color = Color.rgb(3, 3, 5)
        line.strokeWidth = 3f
        c.drawPath(p, line)

        fill.color = light
        val leftLapel = Path().apply {
            moveTo(-24f, -2f); lineTo(-5f, 5f); lineTo(-1f, 45f); lineTo(-17f, 27f); close()
        }
        val rightLapel = Path().apply {
            moveTo(24f, -2f); lineTo(5f, 5f); lineTo(1f, 45f); lineTo(17f, 27f); close()
        }
        c.drawPath(leftLapel, fill)
        c.drawPath(rightLapel, fill)

        line.color = Color.rgb(58, 58, 64)
        line.strokeWidth = 1.5f
        c.drawLine(0f, 5f, 0f, bottom - 4f, line)
        for (i in 0..3) {
            fill.color = Color.rgb(125, 125, 133)
            c.drawCircle(0f, 15f + i * 12f, 1.6f, fill)
        }
        line.color = Color.rgb(55, 55, 61)
        line.strokeWidth = 2f
        c.drawLine(-30f, 25f, -34f, bottom + 4f, line)
        c.drawLine(30f, 25f, 34f, bottom + 4f, line)
    }

    private fun drawCiroCoat(c: Canvas, white: Int, shadow: Int, sitting: Boolean) {
        val bottom = if (sitting) 65f else 72f
        val p = Path().apply {
            moveTo(-28f, -3f)
            cubicTo(-35f, 15f, -34f, 40f, -29f, bottom)
            cubicTo(-18f, bottom + 3f, -8f, bottom + 2f, 0f, bottom + 2f)
            cubicTo(8f, bottom + 2f, 18f, bottom + 3f, 29f, bottom)
            cubicTo(34f, 40f, 35f, 15f, 28f, -3f)
            close()
        }
        fill.color = white
        c.drawPath(p, fill)
        line.color = Color.rgb(37, 37, 41)
        line.strokeWidth = 2.8f
        c.drawPath(p, line)

        fill.color = shadow
        val left = Path().apply { moveTo(-22f, -1f); lineTo(-4f, 6f); lineTo(-1f, 43f); lineTo(-16f, 27f); close() }
        val right = Path().apply { moveTo(22f, -1f); lineTo(4f, 6f); lineTo(1f, 43f); lineTo(16f, 27f); close() }
        c.drawPath(left, fill)
        c.drawPath(right, fill)

        line.color = Color.rgb(102, 101, 105)
        line.strokeWidth = 1.4f
        c.drawLine(0f, 6f, 0f, bottom - 3f, line)
        for (i in 0..3) {
            fill.color = Color.rgb(93, 92, 98)
            c.drawCircle(0f, 15f + i * 12f, 1.6f, fill)
        }
        line.color = Color.rgb(160, 159, 157)
        line.strokeWidth = 2f
        c.drawLine(-28f, 25f, -32f, bottom + 3f, line)
        c.drawLine(28f, 25f, 32f, bottom + 3f, line)
    }

    private fun drawArms(c: Canvas, skin: Int, sleeve: Int, glove: Int, swing: Float, sitting: Boolean, alex: Boolean) {
        val shoulderY = 0f
        val length = if (sitting) 34f else 42f
        val leftAngle = -14f + swing
        val rightAngle = 14f - swing
        drawArm(c, -27f, shoulderY, leftAngle, length, sleeve, glove, left = true)
        drawArm(c, 27f, shoulderY, rightAngle, length, sleeve, glove, left = false)
    }

    private fun drawArm(c: Canvas, x: Float, y: Float, angle: Float, length: Float, sleeve: Int, skin: Int, left: Boolean) {
        val sign = if (left) -1f else 1f
        val rad = Math.toRadians(angle.toDouble())
        val outward = cos(rad).toFloat() * sign
        val vertical = sin(rad).toFloat()
        val elbowX = x + outward * length * 0.48f
        val elbowY = y + vertical * length * 0.48f + 7f
        val handX = x + outward * length
        val handY = y + vertical * length + 10f

        line.color = Color.rgb(6, 6, 8)
        line.strokeWidth = 12f
        c.drawLine(x, y, elbowX, elbowY, line)
        c.drawLine(elbowX, elbowY, handX, handY, line)
        line.color = sleeve
        line.strokeWidth = 9f
        c.drawLine(x, y, elbowX, elbowY, line)
        line.color = skin
        line.strokeWidth = 7.5f
        c.drawLine(elbowX, elbowY, handX, handY, line)
    }

    private fun drawHands(c: Canvas, skin: Int, sleeve: Int, swing: Float, sitting: Boolean, alex: Boolean) {
        val length = if (sitting) 34f else 42f
        val leftAngle = -14f + swing
        val rightAngle = 14f - swing
        drawHand(c, -27f, 0f, leftAngle, length, skin, true)
        drawHand(c, 27f, 0f, rightAngle, length, skin, false)
    }

    private fun drawHand(c: Canvas, x: Float, y: Float, angle: Float, length: Float, skin: Int, left: Boolean) {
        val sign = if (left) -1f else 1f
        val rad = Math.toRadians(angle.toDouble())
        val handX = x + cos(rad).toFloat() * sign * length
        val handY = y + sin(rad).toFloat() * length + 10f
        fill.color = skin
        c.drawOval(RectF(handX - 5.5f, handY - 4.5f, handX + 5.5f, handY + 6.5f), fill)
    }

    private fun drawHead(c: Canvas, skin: Int, light: Int, shadow: Int, hair: Int, hairShadow: Int, eyeColor: Int, frame: Int, hairMotion: Float, alex: Boolean) {
        fill.color = skin
        c.drawRoundRect(RectF(-9f, -15f, 9f, 5f), 5f, 5f, fill)

        val face = Path().apply {
            moveTo(-28f, -57f)
            cubicTo(-28f, -75f, -16f, -86f, 0f, -87f)
            cubicTo(16f, -86f, 28f, -75f, 28f, -57f)
            cubicTo(27f, -38f, 17f, -23f, 0f, -20f)
            cubicTo(-17f, -23f, -27f, -38f, -28f, -57f)
            close()
        }
        fill.color = skin
        c.drawPath(face, fill)
        line.color = shadow
        line.strokeWidth = 1.8f
        c.drawPath(face, line)

        if (alex) drawAlexFrontHair(c, hair, hairShadow, hairMotion)
        else drawCiroFrontCurls(c, hair, hairShadow, hairMotion)
        drawEyes(c, eyeColor, frame, alex)

        fill.color = if (alex) Color.rgb(151, 35, 46) else Color.rgb(105, 67, 49)
        val nose = Path().apply { moveTo(1f, -51f); lineTo(-2f, -39f); lineTo(3f, -38f); close() }
        c.drawPath(nose, fill)
    }

    private fun drawAlexFrontHair(c: Canvas, hair: Int, shadow: Int, motion: Float) {
        fill.color = hair
        val fringe = Path().apply {
            moveTo(-29f, -59f)
            cubicTo(-28f, -79f, -16f, -92f, 0f, -93f)
            cubicTo(17f, -92f, 30f, -80f, 29f, -59f)
            cubicTo(23f, -64f, 20f, -72f, 15f, -69f)
            cubicTo(9f, -65f, 7f, -75f, 1f, -68f)
            cubicTo(-5f, -61f, -10f, -76f, -16f, -68f)
            cubicTo(-21f, -62f, -24f, -64f, -29f, -59f)
            close()
        }
        c.drawPath(fringe, fill)
        line.color = shadow
        line.strokeWidth = 1.5f
        for (i in 0..6) {
            val x = -24f + i * 8f
            c.drawLine(x, -84f, x + motion * 0.08f, -58f, line)
        }
    }

    private fun drawCiroFrontCurls(c: Canvas, hair: Int, highlight: Int, motion: Float) {
        fill.color = hair
        val cap = Path().apply {
            moveTo(-29f, -59f)
            cubicTo(-29f, -79f, -16f, -91f, 0f, -91f)
            cubicTo(16f, -91f, 29f, -79f, 29f, -59f)
            cubicTo(24f, -65f, 20f, -60f, 16f, -66f)
            cubicTo(11f, -73f, 8f, -64f, 3f, -69f)
            cubicTo(-2f, -75f, -5f, -64f, -11f, -69f)
            cubicTo(-17f, -75f, -20f, -63f, -25f, -65f)
            close()
        }
        c.drawPath(cap, fill)

        line.color = highlight
        line.strokeWidth = 2.1f
        val curls = arrayOf(
            floatArrayOf(-22f, -73f), floatArrayOf(-12f, -79f), floatArrayOf(-2f, -75f),
            floatArrayOf(9f, -80f), floatArrayOf(19f, -72f), floatArrayOf(-25f, -57f),
            floatArrayOf(25f, -56f)
        )
        curls.forEachIndexed { index, point ->
            val x = point[0]
            val y = point[1] + motion * (if (index % 2 == 0) 0.12f else -0.08f)
            c.drawArc(RectF(x - 7f, y - 7f, x + 7f, y + 7f), 20f, 270f, false, line)
        }
    }

    private fun drawEyes(c: Canvas, eyeColor: Int, frame: Int, alex: Boolean) {
        val blink = frame % 12 == 5 || frame % 12 == 6
        if (blink) {
            line.color = Color.rgb(62, 45, 44)
            line.strokeWidth = 2.2f
            c.drawLine(-18f, -53f, -8f, -52f, line)
            c.drawLine(8f, -52f, 18f, -53f, line)
            return
        }

        line.color = Color.rgb(53, 42, 43)
        line.strokeWidth = 2.1f
        c.drawArc(RectF(-21f, -59f, -7f, -47f), 192f, 155f, false, line)
        c.drawArc(RectF(7f, -59f, 21f, -47f), 193f, 155f, false, line)

        fill.color = Color.rgb(250, 248, 244)
        c.drawOval(RectF(-19f, -56f, -8f, -48f), fill)
        c.drawOval(RectF(8f, -56f, 19f, -48f), fill)

        fill.color = eyeColor
        c.drawOval(RectF(-17f, -55f, -10f, -49f), fill)
        c.drawOval(RectF(10f, -55f, 17f, -49f), fill)

        fill.color = Color.rgb(20, 18, 20)
        c.drawOval(RectF(-14.8f, -54.5f, -11.2f, -49.5f), fill)
        c.drawOval(RectF(12.2f, -54.5f, 15.8f, -49.5f), fill)

        fill.color = Color.WHITE
        c.drawCircle(-14f, -53f, 0.8f, fill)
        c.drawCircle(13f, -53f, 0.8f, fill)
    }

    private fun drawAlexFaceDetails(c: Canvas, darkRed: Int) {
        line.color = Color.rgb(86, 65, 66)
        line.strokeWidth = 1.5f
        c.drawLine(-7f, -29f, 7f, -29f, line)
        fill.color = darkRed
        c.drawOval(RectF(-1.5f, -30.5f, 1.5f, -28f), fill)
    }

    private fun drawCiroFaceDetails(c: Canvas, dark: Int, silver: Int) {
        line.color = Color.rgb(79, 48, 39)
        line.strokeWidth = 1.4f
        c.drawLine(-7f, -29f, 7f, -29f, line)
        line.color = dark
        line.strokeWidth = 2.2f
        c.drawArc(RectF(-10f, -35f, 10f, -24f), 10f, 160f, false, line)

        // Small white stud on Ciro's left ear, matching the character sheet.
        fill.color = Color.WHITE
        c.drawCircle(-26.5f, -43f, 2.2f, fill)
        line.color = silver
        line.strokeWidth = 1f
        c.drawCircle(-26.5f, -43f, 2.7f, line)
    }

    private fun drawHeld(c: Canvas, kind: String, frame: Int) {
        val sway = sin(frame * Math.PI * 2.0 / 12.0).toFloat() * 1.5f
        c.save()
        c.rotate(sway, 0f, 28f)
        drawHeldCharacter(c, kind, frame)
        c.restore()
    }

    private fun drawHeldCharacter(c: Canvas, kind: String, frame: Int) {
        val heldBob = sin(frame * Math.PI * 2.0 / 12.0).toFloat() * 0.8f
        c.translate(0f, heldBob)
        if (kind == "ALEX") {
            drawAlex(c, frame, PetBehavior.State.HELD, false, 0f, 0f, 0f)
        } else {
            drawCiro(c, frame, PetBehavior.State.HELD, false, 0f, 0f, 0f)
        }
    }

    private fun walkPhase(frame: Int): Float = sin(frame * Math.PI * 2.0 / 16.0).toFloat()
    private fun runPhase(frame: Int): Float = sin(frame * Math.PI * 2.0 / 18.0).toFloat()
    private fun walkBob(frame: Int): Float = absWave(frame, 16) * 1.0f
    private fun runBob(frame: Int): Float = absWave(frame, 18) * 1.25f
    private fun walkLean(frame: Int): Float = sin(frame * Math.PI * 2.0 / 16.0).toFloat() * 0.35f
    private fun runLean(frame: Int): Float = -1.4f + sin(frame * Math.PI * 2.0 / 18.0).toFloat() * 0.25f
    private fun jumpY(frame: Int): Float = floatArrayOf(0f, -2f, -6f, -11f, -16f, -20f, -22f, -20f, -16f, -11f, -5f, -1f)[frame % 12]
    private fun jumpLean(frame: Int): Float = floatArrayOf(0f, -0.4f, -0.8f, -1.1f, -0.8f, 0f, 0.5f, 0.8f, 0.5f, 0.2f, 0f, 0f)[frame % 12]
    private fun fallY(frame: Int): Float = floatArrayOf(-7f, -5f, -2f, 2f, 7f, 13f, 19f, 24f, 28f, 31f, 33f, 34f, 33f, 31f)[frame % 14]
    private fun fallLean(frame: Int): Float = sin(frame * Math.PI / 7.0).toFloat() * 2.2f
    private fun fallRotation(frame: Int): Float = floatArrayOf(0f, -5f, -10f, -16f, -22f, -27f, -31f, -34f, -29f, -22f, -14f, -7f, -2f, 0f)[frame % 14]
    private fun landingY(frame: Int): Float = floatArrayOf(6f, 6f, 5f, 4f, 3f, 2f, 1f, 0f, 0f, 0f, 0f, 0f)[frame % 12]
    private fun landingSquash(frame: Int): Float = floatArrayOf(0.84f, 0.87f, 0.90f, 0.93f, 0.96f, 0.98f, 0.99f, 1f, 1f, 1f, 1f, 1f)[frame % 12]
    private fun absWave(frame: Int, count: Int): Float = abs(sin(frame * Math.PI * 2.0 / count).toFloat())
}
