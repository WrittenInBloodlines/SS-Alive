package com.ss.alive.alive

import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TemplatesActivity : AppCompatActivity() {

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showTemplates()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) showTemplates()
    }

    private fun showTemplates() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "TEMPLATES"
            textSize = 30f
        })

        root.addView(TextView(this).apply {
            text = "Ready-made Alives you can equip and place on your screen."
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        addTemplate("CAT", "Cat")
        addTemplate("DOG", "Dog")
        addTemplate("CHICK", "Chick")

        root.addView(Button(this).apply {
            text = "BACK"
            setOnClickListener { finish() }
        })

        setContentView(root)
    }

    private fun addTemplate(kind: String, name: String) {
        val profile = AliveRepository.template(this, kind)

        root.addView(TemplatePreviewView(this, kind).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                260
            )
        })

        root.addView(TextView(this).apply {
            text = name
            textSize = 22f
        })

        root.addView(Button(this).apply {
            text = if (AliveRepository.isEquipped(this@TemplatesActivity, profile.id)) {
                "UNEQUIP"
            } else {
                "EQUIP"
            }

            setOnClickListener {
                if (AliveRepository.isEquipped(this@TemplatesActivity, profile.id)) {
                    AliveRepository.unequip(this@TemplatesActivity, profile.id)
                } else {
                    AliveRepository.equip(this@TemplatesActivity, profile)
                }
                showTemplates()
            }
        })
    }
}

private class TemplatePreviewView(
    context: android.content.Context,
    private val kind: String
) : View(context) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
        textSize = 18f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val previewSize = minOf(width, height) * 0.78f
        val left = (width - previewSize) / 2f
        val top = (height - previewSize) / 2f

        canvas.save()
        canvas.translate(left, top)
        canvas.clipRect(0f, 0f, previewSize, previewSize)

        BuiltInTemplateSprite.draw(
            kind,
            canvas,
            0,
            PetBehavior.State.IDLE,
            1
        )

        canvas.restore()

        canvas.drawText(
            kind.lowercase().replaceFirstChar { it.uppercase() } + " preview",
            width / 2f,
            height - 12f,
            labelPaint
        )
    }
}
