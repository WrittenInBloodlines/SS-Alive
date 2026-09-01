package com.ss.alive.alive

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs

/** Built-in Cat Template. */
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
        PetBehavior.State.WALKING -> 12
        PetBehavior.State.CLIMBING -> 10
        PetBehavior.State.RUNNING -> 8
        PetBehavior.State.SIT -> 7
        PetBehavior.State.JUMP -> 8
        PetBehavior.State.FALLING -> 12
        PetBehavior.State.LANDING -> 6
        PetBehavior.State.HELD -> 8
    }

    fun draw(canvas: Canvas, frame: Int, state: PetBehavior.State, direction: Int) {
        val baseScale = minOf(canvas.width, canvas.height) / 150f
        canvas.save()
        canvas.scale(baseScale, baseScale)
        canvas.translate(75f, 78f)
        if (direction < 0) canvas.scale(-1f, 1f)
        when (state) {
            PetBehavior.State.IDLE -> idle(canvas, frame)
            PetBehavior.State.WALKING -> walk(canvas, frame)
            PetBehavior.State.CLIMBING -> climb(canvas, frame)
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
        val bob = floatArrayOf(0f,0f,-0.7f,-1.2f,-0.5f,0.5f,1f,0.4f,-0.4f,-0.8f,0f,0f)[f]
        val tail = intArrayOf(0,0,0,1,1,2,2,1,0,0,0,0)[f]
        val face = when (f) { 4,5 -> 1; 7,8 -> 2; else -> 0 }
        drawCat(c,bob,0f,tail,face,1.14f,false,0)
    }

    private fun walk(c: Canvas, frame: Int) {
        val f = frame.mod(12)
        val bob = floatArrayOf(0.7f,0f,-0.9f,-1.5f,-1.9f,-1.3f,-0.4f,0.4f,0.8f,0.4f,-0.1f,0.3f)[f]
        val pitch = floatArrayOf(0.7f,0.4f,0f,-0.4f,-0.7f,-0.5f,-0.1f,0.3f,0.6f,0.4f,0f,0.3f)[f]
        val tail = intArrayOf(1,1,0,0,3,3,2,2,1,1,0,0)[f]
        drawCat(c,bob,pitch,tail,0,1.14f,false,f)
    }

    private fun climb(c: Canvas, frame: Int) {
        val f = frame.mod(10)
        val reach = floatArrayOf(0f,3f,6f,8f,6f,2f,-2f,-5f,-7f,-3f)[f]
        val sway = floatArrayOf(-1f,0f,1f,2f,1f,0f,-1f,-2f,-1f,0f)[f]
        c.save()
        c.rotate(-90f)
        c.translate(reach * 0.25f, sway)
        drawCat(c,0f,0f,f % 4,0,1.10f,false,60 + f)
        c.restore()
    }

    private fun run(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val bob = floatArrayOf(1.5f,-0.5f,-2f,-0.5f,1.5f,-0.5f,-2f,-0.5f)[f]
        val pitch = floatArrayOf(2f,1f,-1f,-2f,2f,1f,-1f,-2f)[f]
        drawCat(c,bob,pitch,f % 3,0,1.14f,false,20+f)
    }

    private fun sit(c: Canvas, frame: Int) {
        val f = frame.mod(7)
        val bob = floatArrayOf(1f,0f,-0.8f,0f,0.8f,0f,0f)[f]
        drawCat(c,bob,0f,f % 3,if (f == 5) 1 else 0,1.14f,true,0)
    }

    private fun jump(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val y = floatArrayOf(1f,-2f,-6f,-10f,-11f,-8f,-4f,0f)[f]
        val rotation = floatArrayOf(-2f,-4f,-3f,0f,2f,4f,3f,1f)[f]
        c.save(); c.rotate(rotation)
        drawCat(c,y,0f,f,0,1.14f,false,30+f)
        c.restore()
    }

    private fun fall(c: Canvas, frame: Int) {
        val f = frame.mod(12)
        val rotation = floatArrayOf(0f,-25f,-55f,-90f,-125f,-160f,-195f,-230f,-270f,-305f,-335f,-360f)[f]
        val y = floatArrayOf(0f,1f,2f,3f,5f,7f,8f,8f,7f,5f,3f,1f)[f]
        val stretch = floatArrayOf(1f,1f,1.01f,1.03f,1.04f,1.02f,1f,0.98f,0.97f,0.98f,1f,1f)[f]
        c.save(); c.rotate(rotation); c.scale(stretch,2f-stretch)
        drawCat(c,y,0f,f % 3,if (f == 8) 2 else 0,1.14f,false,40+f)
        c.restore()
    }

    private fun landing(c: Canvas, frame: Int) {
        val f = frame.mod(6)
        val squash = floatArrayOf(.72f,.82f,.90f,.96f,.99f,1f)[f]
        val y = floatArrayOf(9f,7f,5f,3f,1f,0f)[f]
        c.save(); c.scale(1f+(1f-squash)*.16f,squash)
        drawCat(c,y,0f,f,0,1.14f,false,0)
        c.restore()
    }

    private fun held(c: Canvas, frame: Int) {
        val f = frame.mod(8)
        val sway = floatArrayOf(-2f,-1.5f,-1f,0f,1f,1.5f,1f,0f)[f]
        c.save(); c.rotate(sway)
        drawCat(c,floatArrayOf(0f,.5f,1f,.5f,0f,-.5f,-1f,-.5f)[f],0f,f % 3,if (f == 4) 2 else 0,1.14f,false,0)
        c.restore()
    }

    private fun drawCat(c: Canvas,bob: Float,bodyPitch: Float,tailPose: Int,facePose: Int,scale: Float,sitting: Boolean,gait: Int) {
        c.save(); c.scale(scale,scale); c.rotate(bodyPitch,0f,18f+bob)
        val tailPath = Path().apply {
            // Tail stays behind the cat: default/right-facing cat has its tail on the left.
            // Canvas mirroring automatically moves it behind the cat when facing left.
            moveTo(-29f,13f+bob)
            when (tailPose.mod(4)) {
                0 -> cubicTo(-37f,10f+bob,-43f,4f+bob,-42f,-3f+bob)
                1 -> cubicTo(-38f,13f+bob,-45f,9f+bob,-45f,2f+bob)
                2 -> cubicTo(-37f,16f+bob,-45f,19f+bob,-42f,25f+bob)
                else -> cubicTo(-38f,12f+bob,-45f,15f+bob,-44f,21f+bob)
            }
        }
        line.color=Color.rgb(45,45,50); line.strokeWidth=8f; c.drawPath(tailPath,line)
        line.color=Color.WHITE; line.strokeWidth=5.2f; c.drawPath(tailPath,line)

        fill.color=Color.WHITE; line.color=Color.rgb(45,45,50); line.strokeWidth=5f
        val body=RectF(-34f,-5f+bob,31f,36f+bob)
        c.drawRoundRect(body,17f,17f,line); c.drawRoundRect(body,17f,17f,fill)

        fill.color=Color.rgb(247,247,249)
        val chest=Path().apply { moveTo(0f,bob); cubicTo(-7f,10f+bob,-6f,25f+bob,0f,34f+bob); cubicTo(7f,25f+bob,8f,10f+bob,0f,bob); close() }
        c.drawPath(chest,fill)

        val head=RectF(-35f,-53f+bob,36f,5f+bob)
        fill.color=Color.WHITE; line.color=Color.rgb(45,45,50); line.strokeWidth=5f
        c.drawRoundRect(head,21f,21f,line); c.drawRoundRect(head,21f,21f,fill)

        drawEar(c,-29f,-42f+bob,-25f,-57f+bob,-9f,-45f+bob)
        drawEar(c,9f,-45f+bob,25f,-57f+bob,30f,-41f+bob)
        fill.color=Color.WHITE
        val leftInner=Path().apply { moveTo(-24f,-44f+bob); lineTo(-24.5f,-51f+bob); lineTo(-16f,-45f+bob); close() }
        val rightInner=Path().apply { moveTo(16f,-45f+bob); lineTo(24.5f,-51f+bob); lineTo(25f,-43f+bob); close() }
        c.drawPath(leftInner,fill); c.drawPath(rightInner,fill)

        fill.color=Color.BLACK
        if (facePose==1) {
            line.color=Color.BLACK; line.strokeWidth=2.5f
            c.drawLine(-18f,-21f+bob,-9f,-22f+bob,line); c.drawLine(9f,-22f+bob,18f,-21f+bob,line)
        } else {
            val eyeHeight=if(facePose==2) 12f else 14f
            c.drawOval(RectF(-20f,-27f+bob,-9f,-27f+eyeHeight+bob),fill)
            c.drawOval(RectF(9f,-27f+bob,20f,-27f+eyeHeight+bob),fill)
            fill.color=Color.WHITE
            c.drawOval(RectF(-16.5f,-25.5f+bob,-14.4f,-21.5f+bob),fill)
            c.drawOval(RectF(12.2f,-25.5f+bob,14.3f,-21.5f+bob),fill)
            fill.color=Color.BLACK
            c.drawOval(RectF(-15.3f,-26f+bob,-13.5f,-18f+bob),fill)
            c.drawOval(RectF(13f,-26f+bob,14.8f,-18f+bob),fill)
        }

        fill.color=Color.rgb(45,45,50); c.drawOval(RectF(-3f,-7f+bob,3f,-2f+bob),fill)
        line.color=Color.rgb(45,45,50); line.strokeWidth=1.5f
        c.drawLine(0f,-2f+bob,-4f,1f+bob,line); c.drawLine(0f,-2f+bob,4f,1f+bob,line)
        line.strokeWidth=1.1f
        c.drawLine(-5f,-4f+bob,-23f,-7f+bob,line); c.drawLine(-5f,-1f+bob,-23f,1f+bob,line)
        c.drawLine(5f,-4f+bob,23f,-7f+bob,line); c.drawLine(5f,-1f+bob,23f,1f+bob,line)

        drawLegs(c,bob,sitting,gait)
        c.restore()
    }

    private fun drawLegs(c: Canvas,bob: Float,sitting: Boolean,gait: Int) {
        val hips=floatArrayOf(-23f,-8f,9f,24f)
        val topY=25f+bob
        val baseY=if(sitting)42f+bob else 46f+bob
        val swing: FloatArray=when {
            sitting -> floatArrayOf(0f,0f,0f,0f)
            gait>=60 -> {
                val f=(gait-60).mod(10)
                val v=floatArrayOf(7f,11f,8f,3f,-3f,-8f,-11f,-7f,-2f,3f)[f]
                floatArrayOf(v,-v*.75f,-v,v*.75f)
            }
            gait>=20 -> {
                val f=(gait-20).mod(8)
                val v=floatArrayOf(12f,7f,-2f,-10f,-12f,-7f,2f,10f)[f]
                floatArrayOf(v,-v*.72f,-v,v*.72f)
            }
            gait>=30 -> {
                val f=(gait-30).mod(8)
                val v=floatArrayOf(7f,-6f,6f,-7f,7f,-6f,6f,-7f)[f]
                floatArrayOf(v,-v,-v,v)
            }
            else -> {
                val f=gait.mod(12)
                val v=floatArrayOf(9f,12f,9f,4f,-2f,-8f,-11f,-8f,-3f,3f,7f,8f)[f]
                floatArrayOf(v,-v*.65f,-v,v*.65f)
            }
        }
        for(i in 0..3) {
            val x=hips[i]
            val s=swing[i]
            val kneeX=x+s*0.48f
            val kneeY=topY+9f-abs(s)*0.12f
            val footX=x+s
            val footY=baseY-abs(s)*0.18f
            line.color=Color.rgb(45,45,50); line.strokeWidth=8f
            c.drawLine(x,topY,kneeX,kneeY,line)
            c.drawLine(kneeX,kneeY,footX,footY,line)
            line.color=Color.WHITE; line.strokeWidth=5.2f
            c.drawLine(x,topY,kneeX,kneeY,line)
            c.drawLine(kneeX,kneeY,footX,footY,line)
            c.drawLine(footX-2.5f,footY,footX+3.5f,footY,line)
        }
    }

    private fun drawEar(c: Canvas,x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float) {
        val p=Path().apply { moveTo(x1,y1); lineTo(x2,y2); lineTo(x3,y3); close() }
        fill.color=Color.WHITE; line.color=Color.rgb(45,45,50); line.strokeWidth=5f
        c.drawPath(p,line); c.drawPath(p,fill)
    }
}
