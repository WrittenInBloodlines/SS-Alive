package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Built-in cat template. White fur, natural black facial features, soft outline and state-specific animation. */
object PetSprite {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    fun frameCount(state: PetBehavior.State): Int = when (state) {
        PetBehavior.State.IDLE -> 12
        PetBehavior.State.WALKING -> 10
        PetBehavior.State.RUNNING -> 8
        PetBehavior.State.SIT -> 7
        PetBehavior.State.JUMP -> 8
        PetBehavior.State.FALLING -> 8
        PetBehavior.State.LANDING -> 6
        PetBehavior.State.HELD -> 8
    }

    fun draw(canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        val scale = minOf(canvas.width, canvas.height) / 150f
        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(75f, 82f)
        // The sprite faces right by default. Its tail remains on the left/back side.
        if (direction < 0) canvas.scale(-1f, 1f)
        when (state) {
            PetBehavior.State.IDLE -> idle(canvas, frame)
            PetBehavior.State.WALKING -> walk(canvas, frame)
            PetBehavior.State.RUNNING -> run(canvas, frame)
            PetBehavior.State.SIT -> sit(canvas, frame)
            PetBehavior.State.JUMP -> jump(canvas, frame)
            PetBehavior.State.FALLING -> fall(canvas, frame)
            PetBehavior.State.LANDING -> landing(canvas, frame)
            PetBehavior.State.HELD -> held(canvas, frame)
        }
        canvas.restore()
    }

    private fun idle(c: Canvas, frame: Int) {
        val f = frame.mod(12)
        val bob = floatArrayOf(0f, 0f, -1f, -1f, 0f, 1f, 1f, 0f, -1f, 0f, 1f, 0f)[f]
        val tail = intArrayOf(0,0,1,1,2,2,3,3,2,1,0,0)[f]
        val face = when (f) {
            4, 5 -> 1 // slow blink
            7 -> 2 // brief curious look
            else -> 0
        }
        drawCat(c, bob, 0f, tail, face, 1f, false)
    }

    private fun walk(c: Canvas, frame: Int) {
        val f = frame.mod(10)
        val bob = floatArrayOf(1f,0f,-1f,-2f,-1f,0f,1f,2f,1f,0f)[f]
        val legs = floatArrayOf(7f,4f,0f,-6f,-8f,-4f,2f,7f,6f,2f)[f]
        val tail = intArrayOf(0,0,1,1,2,2,1,1,0,0)[f]
        drawCat(c, bob, legs, tail, 0, 1f, false)
    }

    private fun run(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val bob = floatArrayOf(2f,0f,-2f,-1f,2f,0f,-2f,0f)[f]
        val legs = floatArrayOf(12f,-12f,10f,-10f,12f,-12f,9f,-9f)[f]
        drawCat(c, bob, legs, f, 0, 1.01f, false)
    }

    private fun sit(c: Canvas, frame: Int) {
        val f = frame.mod(7)
        val bob = floatArrayOf(1f,0f,-1f,0f,1f,0f,0f)[f]
        drawCat(c, bob, 0f, f % 4, if (f == 5) 1 else 0, 1f, true)
    }

    private fun jump(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val y = floatArrayOf(1f,-2f,-6f,-10f,-10f,-7f,-3f,0f)[f]
        val rotation = floatArrayOf(-3f,-4f,-2f,0f,2f,4f,3f,1f)[f]
        c.save(); c.rotate(rotation)
        drawCat(c, y, floatArrayOf(7f,5f,2f,-7f,-9f,-5f,3f,7f)[f], f, 0, 1f, false)
        c.restore()
    }

    private fun fall(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val rotation = floatArrayOf(0f,-8f,-18f,-30f,-42f,-54f,-64f,-72f)[f]
        val y = floatArrayOf(0f,2f,4f,6f,8f,10f,11f,12f)[f]
        val legs = floatArrayOf(0f,5f,-5f,9f,-8f,7f,-6f,2f)[f]
        c.save(); c.rotate(rotation)
        drawCat(c, y, legs, f + 1, if (f == 6) 1 else 0, 1f, false)
        c.restore()
    }

    private fun landing(c: Canvas, frame: Int) {
        val f = frame.mod(6)
        val squash = floatArrayOf(.70f,.80f,.89f,.95f,.99f,1f)[f]
        val y = floatArrayOf(9f,7f,5f,3f,1f,0f)[f]
        c.save(); c.scale(1f + (1f - squash) * .18f, squash)
        drawCat(c, y, 0f, f, 0, 1f, false)
        c.restore()
    }

    private fun held(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val sway = floatArrayOf(-3f,-2f,-1f,0f,2f,3f,2f,0f)[f]
        c.save(); c.rotate(sway)
        drawCat(c, floatArrayOf(0f,1f,2f,1f,0f,-1f,-2f,-1f)[f], 0f, f % 4, if (f == 4) 1 else 0, .98f, false)
        c.restore()
    }

    private fun drawCat(c: Canvas, bob: Float, legOffset: Float, tailPose: Int, facePose: Int, scale: Float, sitting: Boolean) {
        c.save(); c.scale(scale, scale)
        // Tail is behind the body. It stays on the back/left when the cat faces right.
        line.color = Color.BLACK; line.strokeWidth = 8f
        val tail = Path().apply {
            moveTo(28f, 12f + bob)
            when (tailPose.mod(4)) {
                0 -> cubicTo(39f, 7f + bob, 47f, 0f + bob, 43f, -9f + bob)
                1 -> cubicTo(41f, 10f + bob, 49f, 7f + bob, 47f, -1f + bob)
                2 -> cubicTo(42f, 17f + bob, 49f, 25f + bob, 40f, 32f + bob)
                else -> cubicTo(40f, 10f + bob, 48f, 17f + bob, 43f, 24f + bob)
            }
        }
        c.drawPath(tail, line)

        fill.color = Color.WHITE
        line.color = Color.rgb(35,35,40); line.strokeWidth = 5.5f
        val body = RectF(-33f,-5f+bob,31f,36f+bob)
        c.drawRoundRect(body,17f,17f,line); c.drawRoundRect(body,17f,17f,fill)

        val chest = Path().apply { moveTo(0f, 0f+bob); cubicTo(-6f,10f+bob,-5f,25f+bob,0f,34f+bob); cubicTo(7f,24f+bob,8f,10f+bob,0f,0f+bob); close() }
        fill.color = Color.rgb(248,248,250); c.drawPath(chest, fill)

        val head = RectF(-34f,-53f+bob,35f,4f+bob)
        fill.color = Color.WHITE; line.color = Color.rgb(35,35,40); line.strokeWidth = 5.5f
        c.drawRoundRect(head,21f,21f,line); c.drawRoundRect(head,21f,21f,fill)

        drawEar(c,-28f,-42f+bob,-25f,-59f+bob,-9f,-45f+bob)
        drawEar(c,9f,-45f+bob,25f,-59f+bob,29f,-41f+bob)

        // Ears have white interiors, not black filled triangles.
        fill.color = Color.rgb(232,232,236)
        val li = Path().apply { moveTo(-23f,-45f+bob); lineTo(-24f,-53f+bob); lineTo(-16f,-46f+bob); close() }
        val ri = Path().apply { moveTo(16f,-46f+bob); lineTo(24f,-53f+bob); lineTo(25f,-44f+bob); close() }
        c.drawPath(li,fill); c.drawPath(ri,fill)

        // More feline eyes: narrower, vertical pupils, no oversized anime look.
        fill.color = Color.BLACK
        if (facePose == 1) {
            line.color = Color.BLACK; line.strokeWidth = 2.5f
            c.drawLine(-18f,-21f+bob,-9f,-21f+bob,line); c.drawLine(9f,-21f+bob,18f,-21f+bob,line)
        } else {
            c.drawOval(RectF(-20f,-29f+bob,-9f,-14f+bob),fill)
            c.drawOval(RectF(9f,-29f+bob,20f,-14f+bob),fill)
            fill.color = Color.WHITE
            c.drawOval(RectF(-16.5f,-27f+bob,-14f,-18f+bob),fill)
            c.drawOval(RectF(12f,-27f+bob,14.5f,-18f+bob),fill)
            fill.color = Color.BLACK
            c.drawOval(RectF(-15.2f,-26f+bob,-13.2f,-17f+bob),fill)
            c.drawOval(RectF(13f,-26f+bob,15f,-17f+bob),fill)
        }

        fill.color = Color.rgb(35,35,40)
        c.drawOval(RectF(-2.8f,-7f+bob,2.8f,-2f+bob),fill)
        line.color = Color.rgb(35,35,40); line.strokeWidth = 1.6f
        c.drawLine(0f,-2f+bob,-4f,1f+bob,line); c.drawLine(0f,-2f+bob,4f,1f+bob,line)

        // Subtle whiskers.
        line.strokeWidth = 1.2f
        c.drawLine(-5f,-4f+bob,-24f,-8f+bob,line); c.drawLine(-5f,-1f+bob,-24f,1f+bob,line)
        c.drawLine(5f,-4f+bob,24f,-8f+bob,line); c.drawLine(5f,-1f+bob,24f,1f+bob,line)

        // White legs with only a thin dark outline, as requested.
        line.color = Color.rgb(35,35,40); line.strokeWidth = 8f
        fill.color = Color.WHITE
        val xs = floatArrayOf(-22f,-7f,9f,24f)
        val endY = if (sitting) 42f else 46f
        for (i in 0..3) {
            val off = if (sitting) 0f else legOffset * if (i % 2 == 0) 1f else -1f
            line.strokeWidth = 8f
            c.drawLine(xs[i],26f+bob,xs[i]+off,endY+bob,line)
            line.color = Color.WHITE; line.strokeWidth = 5.2f
            c.drawLine(xs[i],26f+bob,xs[i]+off,endY+bob,line)
            line.color = Color.rgb(35,35,40)
        }
        c.restore()
    }

    private fun drawEar(c: Canvas,x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float) {
        val p = Path().apply { moveTo(x1,y1); lineTo(x2,y2); lineTo(x3,y3); close() }
        fill.color = Color.WHITE; line.color = Color.rgb(35,35,40); line.strokeWidth = 5f
        c.drawPath(p,line); c.drawPath(p,fill)
    }
}
