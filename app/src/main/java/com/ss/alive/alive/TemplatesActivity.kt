package com.ss.alive.alive

import android.os.Bundle
import android.view.Gravity
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
        AliveRepository.removeLegacyTemplates(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "TEMPLATES"
            textSize = 30f
        })
        root.addView(TextView(this).apply {
            text = "No built-in characters yet. Create your own Alive and supply your own animation frames."
            textSize = 16f
            setPadding(0, 20, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "CREATE ALIVE"
            setOnClickListener {
                startActivity(android.content.Intent(this@TemplatesActivity, AliveEditorActivity::class.java))
            }
        })
        root.addView(Button(this).apply {
            text = "BACK"
            setOnClickListener { finish() }
        })
        setContentView(root)
    }
}
