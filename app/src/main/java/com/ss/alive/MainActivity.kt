package com.ss.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
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
import com.ss.alive.ui.AliveUi

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
        val (scroll, content) = AliveUi.scrollPage(this)
        rootView = content

        content.addView(AliveUi.label(this, "S•S  /  SCREEN COMPANIONS"))
        content.addView(AliveUi.gap(this, 8))
        content.addView(AliveUi.heading(this, "S•S Alive", 32f))
        content.addView(AliveUi.body(this, "A little life on your screen.", 16f))
        content.addView(AliveUi.gap(this, 22))

        val welcome = AliveUi.column(this)
        welcome.addView(AliveUi.heading(this, "Make your screen yours.", 21f))
        welcome.addView(AliveUi.body(this, "Create a character, give it animations, and let it explore your home screen."))
        welcome.addView(AliveUi.gap(this, 16))
        AliveUi.addButtonRow(this, welcome, listOf(
            AliveUi.button(this, "Create Alive", true) {
                startActivity(Intent(this, AliveEditorActivity::class.java))
            },
            AliveUi.button(this, "Browse templates", false) {
                startActivity(Intent(this, TemplatesActivity::class.java))
            }
        ))
        content.addView(AliveUi.card(this, welcome, true))

        val setup = AliveUi.column(this)
        setup.addView(AliveUi.label(this, "QUICK SETUP"))
        setup.addView(AliveUi.gap(this, 8))
        setup.addView(AliveUi.heading(this, "Permissions", 19f))
        setup.addView(AliveUi.body(this, "Two Android settings help your Alives appear and interact as intended."))
        setup.addView(AliveUi.gap(this, 12))
        val overlayGranted = Settings.canDrawOverlays(this)
        setup.addView(AliveUi.body(this,
            (if (overlayGranted) "✓  " else "○  ") +
                "Display over other apps — " + if (overlayGranted) "Enabled" else "Needed to show Alives"))
        setup.addView(AliveUi.gap(this, 8))
        setup.addView(AliveUi.button(this,
            if (overlayGranted) "Manage display permission" else "Allow display over other apps",
            false
        ) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        setup.addView(AliveUi.gap(this, 14))
        setup.addView(AliveUi.body(this,
            "Optional: Home-screen surface detection lets Alives land on app icons. It does not read screen text or personal content."))
        setup.addView(AliveUi.gap(this, 8))
        setup.addView(AliveUi.button(this, "Open accessibility settings", false) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(AliveUi.card(this, setup))

        val personalAlives = AliveRepository.all(this).filter { !it.isTemplate }
        val equippedCount = personalAlives.count { AliveRepository.isEquipped(this, it.id) }
        val controls = AliveUi.column(this)
        controls.addView(AliveUi.label(this, "ON YOUR SCREEN"))
        controls.addView(AliveUi.heading(this, "Your Alives", 21f))
        controls.addView(AliveUi.body(this,
            if (personalAlives.isEmpty()) "Your characters will appear here after you create them."
            else "$equippedCount of ${personalAlives.size} equipped"))
        controls.addView(AliveUi.gap(this, 12))
        AliveUi.addButtonRow(this, controls, listOf(
            AliveUi.button(this, "Start equipped", true) { startEquipped() },
            AliveUi.button(this, "Stop all", false) {
                stopService(Intent(this, AliveService::class.java))
                Toast.makeText(this, "S•S Alive paused.", Toast.LENGTH_SHORT).show()
            }
        ))
        content.addView(AliveUi.card(this, controls, true))

        if (personalAlives.isEmpty()) {
            val empty = AliveUi.column(this)
            empty.gravity = android.view.Gravity.START
            empty.addView(AliveUi.heading(this, "No Alives yet", 18f))
            empty.addView(AliveUi.body(this, "Create your first character to get started."))
            empty.addView(AliveUi.gap(this, 10))
            empty.addView(AliveUi.button(this, "Create your first Alive", true) {
                startActivity(Intent(this, AliveEditorActivity::class.java))
            })
            content.addView(AliveUi.card(this, empty))
        } else {
            personalAlives.forEach { profile -> content.addView(aliveCard(profile)) }
        }

        content.addView(AliveUi.gap(this, 8))
        content.addView(AliveUi.body(this, "S•S Alive  •  Made to feel at home on your screen.", 12f))
        setContentView(scroll)
    }

    private fun aliveCard(profile: AliveProfile): android.view.View {
        val equipped = AliveRepository.isEquipped(this, profile.id)
        val card = AliveUi.column(this)
        val top = AliveUi.row(this)
        val details = AliveUi.column(this)
        details.addView(AliveUi.heading(this, profile.name, 19f))
        details.addView(AliveUi.body(this,
            if (equipped) "Equipped • Ready to appear" else "Saved • Not on screen", 13f))
        top.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(top)
        card.addView(AliveUi.gap(this, 14))
        AliveUi.addButtonRow(this, card, listOf(
            AliveUi.button(this, if (equipped) "Unequip" else "Equip", true) {
                if (equipped) AliveRepository.unequip(this, profile.id)
                else AliveRepository.equip(this, profile)
                showHome()
            },
            AliveUi.button(this, "Edit", false) {
                startActivity(Intent(this, AliveEditorActivity::class.java)
                    .putExtra(AliveEditorActivity.EXTRA_ID, profile.id))
            }
        ))
        card.addView(AliveUi.gap(this, 8))
        card.addView(AliveUi.button(this, "More options", false) { showAliveOptions(profile) })
        return AliveUi.card(this, card)
    }

    private fun showAliveOptions(profile: AliveProfile) {
        val latest = AliveRepository.get(this, profile.id) ?: return
        val ghostLabel = if (latest.ghostMode) "Turn Ghost Mode off" else "Turn Ghost Mode on"
        AlertDialog.Builder(this)
            .setTitle(profile.name)
            .setItems(arrayOf(ghostLabel, "Delete Alive")) { _, which ->
                if (which == 0) {
                    latest.ghostMode = !latest.ghostMode
                    AliveRepository.save(this, latest)
                    refreshRunningAlives()
                    showHome()
                } else confirmDelete(profile)
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            Toast.makeText(this, "Equip at least one Alive first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this,
                "Allow display over other apps, then return and press Start equipped.",
                Toast.LENGTH_LONG).show()
            return
        }
        ContextCompat.startForegroundService(this,
            Intent(this, AliveService::class.java).setAction(AliveService.ACTION_SHOW_EQUIPPED))
    }
}
