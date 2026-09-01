package com.ss.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ss.alive.alive.AliveEditorActivity
import com.ss.alive.alive.AliveRepository
import com.ss.alive.alive.AliveService

class MainActivity : AppCompatActivity() {

    private lateinit var rootView: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    override fun onResume() {
        super.onResume()
        if (::rootView.isInitialized) {
            showHome()
        }
    }

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
        })

        rootView.addView(Button(this).apply {
            text = "CREATE ALIVE"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, AliveEditorActivity::class.java))
            }
        })

        rootView.addView(TextView(this).apply {
            text = "TEMPLATES"
            textSize = 22f
            setPadding(0, 28, 0, 8)
        })

        addTemplateCard("CAT", "🐈", "Cat")
        addTemplateCard("DOG", "🐕", "Dog")
        addTemplateCard("CHICK", "🐥", "Chick")

        rootView.addView(Button(this).apply {
            text = "ALLOW DISPLAY OVER OTHER APPS"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        })

        rootView.addView(Button(this).apply {
            text = "START EQUIPPED ALIVES"
            setOnClickListener {
                startEquipped()
            }
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
            })

            personalAlives.forEach { profile ->
                val equipped = AliveRepository.isEquipped(this@MainActivity, profile.id)

                rootView.addView(Button(this).apply {
                    text = if (equipped) {
                        "UNEQUIP " + profile.name.uppercase()
                    } else {
                        "EQUIP " + profile.name.uppercase()
                    }

                    setOnClickListener {
                        if (AliveRepository.isEquipped(this@MainActivity, profile.id)) {
                            AliveRepository.unequip(this@MainActivity, profile.id)
                        } else {
                            AliveRepository.equip(this@MainActivity, profile)
                        }
                        showHome()
                    }
                })
            }
        }

        setContentView(rootView)
    }

    private fun addTemplateCard(
        templateKind: String,
        emoji: String,
        displayName: String
    ) {
        val profile = AliveRepository.template(this, templateKind)

        rootView.addView(TextView(this).apply {
            text = "$emoji  $displayName"
            textSize = 19f
        })

        rootView.addView(Button(this).apply {
            text = if (AliveRepository.isEquipped(this@MainActivity, profile.id)) {
                "UNEQUIP"
            } else {
                "EQUIP"
            }

            setOnClickListener {
                if (AliveRepository.isEquipped(this@MainActivity, profile.id)) {
                    AliveRepository.unequip(this@MainActivity, profile.id)
                } else {
                    AliveRepository.equip(this@MainActivity, profile)
                }
                showHome()
            }
        })
    }

    private fun startEquipped() {
        if (AliveRepository.equipped(this).isEmpty()) {
            Toast.makeText(
                this,
                "Equip at least one Alive first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        ContextCompat.startForegroundService(
            this,
            Intent(this, AliveService::class.java)
        )
    }
}
