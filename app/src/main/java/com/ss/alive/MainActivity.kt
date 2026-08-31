package com.ss.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ss.alive.alive.AliveEditorActivity
import com.ss.alive.alive.AliveProfile
import com.ss.alive.alive.AliveRepository
import com.ss.alive.alive.AliveService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (::rootView.isInitialized) showHome()
    }

    private lateinit var rootView: LinearLayout

    private fun showHome() {
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        rootView.addView(TextView(this).apply {
            text = "S•S ALIVE"
            textSize = 32f
        })
        rootView.addView(TextView(this).apply {
            text = "Create characters that can live on your screen."
            textSize = 17f
            setPadding(0, 16, 0, 28)
        })

        rootView.addView(Button(this).apply {
            text = "CREATE ALIVE"
            setOnClickListener { startActivity(Intent(this@MainActivity, AliveEditorActivity::class.java)) }
        })

        rootView.addView(Button(this).apply {
            text = "USE CAT TEMPLATE"
            setOnClickListener { useCatTemplate() }
        })

        rootView.addView(Button(this).apply {
            text = "ALLOW DISPLAY OVER OTHER APPS"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })

        rootView.addView(Button(this).apply {
            text = "STOP ALIVE"
            setOnClickListener { stopService(Intent(this@MainActivity, AliveService::class.java)) }
        })

        val profiles = AliveRepository.all(this)
        if (profiles.isNotEmpty()) {
            rootView.addView(TextView(this).apply {
                text = "YOUR ALIVES"
                textSize = 21f
                setPadding(0, 28, 0, 10)
            })
            profiles.forEach { profile -> addProfileButton(profile) }
        }

        setContentView(rootView)
    }

    private fun addProfileButton(profile: AliveProfile) {
        rootView.addView(Button(this).apply {
            text = "USE ${profile.name.uppercase()}"
            setOnClickListener { useProfile(profile) }
        })
        rootView.addView(Button(this).apply {
            text = "EDIT ${profile.name.uppercase()}"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AliveEditorActivity::class.java).apply {
                    putExtra(AliveEditorActivity.EXTRA_ID, profile.id)
                })
            }
        })
    }

    private fun useCatTemplate() {
        val profile = AliveRepository.createTemplate(this)
        useProfile(profile)
    }

    private fun useProfile(profile: AliveProfile) {
        AliveRepository.setActive(this, profile)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow overlay access first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        androidx.core.content.ContextCompat.startForegroundService(
            this,
            Intent(this, AliveService::class.java)
        )
    }
}
