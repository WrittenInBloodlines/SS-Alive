package com.ss.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ss.alive.alive.AliveService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        val title = TextView(this).apply {
            text = "S•S ALIVE"
            textSize = 32f
        }
        val subtitle = TextView(this).apply {
            text = "Your characters are alive.\n\nPrototype 0.1: one test pet, overlay, dragging and ghost mode."
            textSize = 17f
            setPadding(0, 24, 0, 32)
        }

        val permission = Button(this).apply {
            text = "Allow display over other apps"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }

        val start = Button(this).apply {
            text = "▶ START ALIVE"
            setOnClickListener { startAlive() }
        }

        val stop = Button(this).apply {
            text = "■ STOP ALIVE"
            setOnClickListener { stopService(Intent(this@MainActivity, AliveService::class.java)) }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(permission)
        root.addView(start)
        root.addView(stop)
        setContentView(root)
    }

    private fun startAlive() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val intent = Intent(this, AliveService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(this, intent)
    }
}
