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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ss.alive.account.AccountBridge
import com.ss.alive.alive.AliveEditorActivity
import com.ss.alive.alive.AliveProfile
import com.ss.alive.alive.AliveRepository
import com.ss.alive.alive.AliveService
import com.ss.alive.alive.TemplatesActivity

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AliveRepository.removeLegacyAlexCiroTemplates(this)
        AccountBridge.request(this)
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (::rootView.isInitialized) showHome()
    }

    private fun showHome() {
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 40)
        }

        rootView.addView(TextView(this).apply {
            text = "S•S ALIVE"
            textSize = 32f
        })

        rootView.addView(TextView(this).apply {
            text = "Create characters that can live on your screen."
            textSize = 17f
            setPadding(0, 0, 0, 20)
        })

        rootView.addView(Button(this).apply {
            text = "CREATE ALIVE"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AliveEditorActivity::class.java))
            }
        })

        rootView.addView(Button(this).apply {
            text = "TEMPLATES"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, TemplatesActivity::class.java))
            }
        })

        rootView.addView(Button(this).apply {
            text = "ALLOW DISPLAY OVER OTHER APPS"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })

        rootView.addView(Button(this).apply {
            text = "OPTIONAL: ENABLE APP SURFACE DETECTION"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        rootView.addView(TextView(this).apply {
            text = "This optional permission lets S•S Alive find visible app interface positions so Alives can sit on them. It uses screen bounds only, not text or personal content. Without it, Alives still work but cannot detect app surfaces."
            textSize = 13f
            setPadding(0, 0, 0, 12)
        })

        rootView.addView(Button(this).apply {
            text = "START EQUIPPED ALIVES"
            setOnClickListener { startEquipped() }
        })

        rootView.addView(Button(this).apply {
            text = "STOP ALIVE"
            setOnClickListener {
                stopService(Intent(this@MainActivity, AliveService::class.java))
            }
        })

        val personalAlives = AliveRepository.all(this).filter { !it.isTemplate }

        if (personalAlives.isNotEmpty()) {
            rootView.addView(TextView(this).apply {
                text = "YOUR ALIVES"
                textSize = 21f
                setPadding(0, 24, 0, 8)
            })

            personalAlives.forEach { profile ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 10, 0, 12)
                }
                card.addView(TextView(this).apply {
                    text = profile.name
                    textSize = 18f
                })

                val primaryRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                primaryRow.addView(Button(this).apply {
                    text = if (AliveRepository.isEquipped(this@MainActivity, profile.id)) "UNEQUIP" else "EQUIP"
                    setOnClickListener {
                        if (AliveRepository.isEquipped(this@MainActivity, profile.id)) {
                            AliveRepository.unequip(this@MainActivity, profile.id)
                        } else {
                            AliveRepository.equip(this@MainActivity, profile)
                        }
                        showHome()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                primaryRow.addView(Button(this).apply {
                    text = "EDIT"
                    setOnClickListener {
                        startActivity(Intent(this@MainActivity, AliveEditorActivity::class.java)
                            .putExtra(AliveEditorActivity.EXTRA_ID, profile.id))
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                card.addView(primaryRow)

                val secondaryRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                secondaryRow.addView(Button(this).apply {
                    text = if (profile.ghostMode) "GHOST: ON" else "GHOST: OFF"
                    setOnClickListener {
                        val latest = AliveRepository.get(this@MainActivity, profile.id) ?: return@setOnClickListener
                        latest.ghostMode = !latest.ghostMode
                        AliveRepository.save(this@MainActivity, latest)
                        refreshRunningAlives()
                        showHome()
                    }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                secondaryRow.addView(Button(this).apply {
                    text = "DELETE"
                    setOnClickListener { confirmDelete(profile) }
                }, LinearLayout.LayoutParams(0, -2, 1f))
                card.addView(secondaryRow)
                rootView.addView(card)
            }
        }

        setContentView(rootView)
    }

    private fun confirmDelete(profile: AliveProfile) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${profile.name}?")
            .setMessage("This permanently removes this Alive and its saved animation settings.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                AliveRepository.delete(this, profile.id)
                refreshRunningAlives()
                showHome()
            }
            .show()
    }

    private fun refreshRunningAlives() {
        if (AliveService.isRunning && Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, AliveService::class.java)
                .setAction(AliveService.ACTION_SHOW_EQUIPPED))
        }
    }

    private fun startEquipped() {
        val equipped = AliveRepository.equipped(this)

        if (equipped.isEmpty()) {
            Toast.makeText(this, "Equip at least one Alive first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(
                this,
                "Allow display over other apps, then return and press Start Equipped Alives.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        ContextCompat.startForegroundService(
            this,
            Intent(this, AliveService::class.java).setAction(AliveService.ACTION_SHOW_EQUIPPED)
        )
    }
}
