package com.ss.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ss.alive.account.AccountBridge
import com.ss.alive.account.AliveAccountStore
import com.ss.alive.alive.AliveEditorActivity
import com.ss.alive.alive.AliveProfile
import com.ss.alive.alive.AliveRepository
import com.ss.alive.alive.AliveService
import com.ss.alive.alive.AliveToolsActivity
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
        if (::rootView.isInitialized) {
            AccountBridge.consumeFeedback(this)?.let { result ->
                val message = when {
                    result == "SUCCESS" -> "Connected to S•S Hub. Your account is up to date."
                    result == "NO_RESPONSE" -> "S•S Hub did not respond. Please try the connection again."
                    result == "ERROR:HUB_UNAVAILABLE" -> "Could not open S•S Hub. Make sure it is installed and try again."
                    result == "ERROR:NOT_SIGNED_IN" -> "You are not signed in to S•S Hub. Sign in there, then refresh the connection."
                    result == "ERROR:EMAIL_NOT_VERIFIED" -> "Your S•S Hub email is not verified yet. Verify it, then try again."
                    result == "ERROR:PROFILE_INVALID" -> "Your S•S Hub profile could not be verified. Please sign in to Hub again."
                    result == "ERROR:PROFILE_UNAVAILABLE" -> "Your S•S Hub profile could not be loaded. Check your connection and try again."
                    result.startsWith("ERROR:") -> "S•S Hub could not confirm your account (${result.removePrefix("ERROR:")}). Please try again."
                    else -> "S•S Hub connection status: $result"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            showHome()
        }
    }

    private fun pageHeader(content: LinearLayout, section: String, title: String, subtitle: String) {
        content.addView(AliveUi.label(this, "S•S ALIVE  /  $section"))
        content.addView(AliveUi.gap(this, 10))
        content.addView(AliveUi.heading(this, title, 29f))
        content.addView(AliveUi.gap(this, 5))
        content.addView(AliveUi.body(this, subtitle, 14f))
        content.addView(AliveUi.gap(this, 25))
    }

    private fun addBack(content: LinearLayout) {
        content.addView(AliveUi.gap(this, 8))
        content.addView(AliveUi.button(this, "Back to menu", false) { showHome() })
    }

    private fun showHome() {
        val (scroll, content) = AliveUi.scrollPage(this)
        rootView = content
        content.gravity = Gravity.TOP
        content.addView(AliveUi.gap(this, 22))
        content.addView(AliveUi.label(this, "S•S  /  SCREEN COMPANIONS"))
        content.addView(AliveUi.gap(this, 10))
        content.addView(AliveUi.heading(this, "Alive", 36f))
        content.addView(AliveUi.body(this, "A little life on your screen.", 14f))
        content.addView(AliveUi.gap(this, 24))

        val accountCard = AliveUi.column(this)
        val connectedAccount = AliveAccountStore.get(this)
        if (connectedAccount != null) {
            val accountName = connectedAccount.displayName.ifBlank { connectedAccount.username }
            accountCard.addView(AliveUi.label(this, "S•S ACCOUNT"))
            accountCard.addView(AliveUi.gap(this, 6))
            accountCard.addView(AliveUi.heading(this, "Connected to S•S Hub", 18f))
            accountCard.addView(AliveUi.body(
                this,
                "Signed in as $accountName" + if (connectedAccount.isCreator) "  •  S•S Creator" else "",
                13f
            ))
            accountCard.addView(AliveUi.gap(this, 12))
            accountCard.addView(AliveUi.button(this, "Refresh account connection", true) {
                refreshAccountConnection()
            })
        } else {
            accountCard.addView(AliveUi.label(this, "S•S ACCOUNT"))
            accountCard.addView(AliveUi.gap(this, 6))
            accountCard.addView(AliveUi.heading(this, "Connect your S•S account", 18f))
            accountCard.addView(AliveUi.body(
                this,
                "S•S Alive uses your S•S Hub session. Open Hub and sign in, then return here to refresh the connection.",
                13f
            ))
            accountCard.addView(AliveUi.gap(this, 12))
            accountCard.addView(AliveUi.button(this, "Open S•S Hub", true) {
                openHubForSignIn()
            })
            accountCard.addView(AliveUi.gap(this, 8))
            accountCard.addView(AliveUi.button(this, "Refresh connection", false) {
                refreshAccountConnection()
            })
        }
        content.addView(AliveUi.card(this, accountCard))
        content.addView(AliveUi.gap(this, 10))

        content.addView(AliveUi.menuTile(this, "My Alives", "Manage, equip and customize your characters.") {
            showAlives()
        })
        content.addView(AliveUi.menuTile(this, "Create an Alive", "Build a character and add its animations.") {
            startActivity(Intent(this, AliveEditorActivity::class.java))
        })
        content.addView(AliveUi.menuTile(this, "Templates", "Browse ready-made characters.") {
            startActivity(Intent(this, TemplatesActivity::class.java))
        })
        content.addView(AliveUi.menuTile(this, "Personality", "Adjust how each Alive moves, rests and interacts.") {
            showPersonality()
        })
        content.addView(AliveUi.menuTile(this, "Alive Studio", "Screen zones, behavior presets, outfits and statistics.") {
            startActivity(Intent(this, AliveToolsActivity::class.java))
        })
        content.addView(AliveUi.menuTile(this, "Settings", "Permissions and screen behavior.") {
            showSettings()
        })

        content.addView(AliveUi.gap(this, 25))
        content.addView(AliveUi.body(this, "S•S ALIVE", 11f))
        setContentView(scroll)
    }

    private fun showAlives() {
        val (scroll, content) = AliveUi.scrollPage(this)
        rootView = content
        pageHeader(content, "MY ALIVES", "My Alives", "Your saved characters and the ones ready for your screen.")
        val personalAlives = AliveRepository.all(this).filter { !it.isTemplate }
        val equippedCount = personalAlives.count { AliveRepository.isEquipped(this, it.id) }

        val status = AliveUi.column(this)
        status.addView(AliveUi.label(this, "CURRENT STATUS"))
        status.addView(AliveUi.gap(this, 5))
        status.addView(AliveUi.heading(this, "$equippedCount / ${personalAlives.size} equipped", 18f))
        status.addView(AliveUi.gap(this, 12))
        AliveUi.addButtonRow(this, status, listOf(
            AliveUi.button(this, "Start equipped", true) { startEquipped() },
            AliveUi.button(this, "Stop all", false) {
                getSharedPreferences(AliveService.RUNTIME_PREFS, MODE_PRIVATE).edit()
                    .putBoolean(AliveService.KEY_AUTO_RESTORE, false)
                    .apply()
                stopService(Intent(this, AliveService::class.java))
                Toast.makeText(this, "S•S Alive paused.", Toast.LENGTH_SHORT).show()
            }
        ))
        content.addView(AliveUi.card(this, status, true))
        content.addView(AliveUi.gap(this, 8))

        if (personalAlives.isEmpty()) {
            val empty = AliveUi.column(this)
            empty.addView(AliveUi.heading(this, "Nothing here yet", 18f))
            empty.addView(AliveUi.body(this, "Create your first Alive to see it here."))
            empty.addView(AliveUi.gap(this, 12))
            empty.addView(AliveUi.button(this, "Create an Alive", true) {
                startActivity(Intent(this, AliveEditorActivity::class.java))
            })
            content.addView(AliveUi.card(this, empty))
        } else {
            personalAlives.forEach { content.addView(aliveCard(it)) }
        }
        addBack(content)
        setContentView(scroll)
    }

    private fun showPersonality() {
        val (scroll, content) = AliveUi.scrollPage(this)
        rootView = content
        pageHeader(content, "PERSONALITY", "Personality", "Choose an Alive to customize its individual behavior.")
        val profiles = AliveRepository.all(this).filter { !it.isTemplate }
        if (profiles.isEmpty()) {
            val empty = AliveUi.column(this)
            empty.addView(AliveUi.heading(this, "No Alives yet", 18f))
            empty.addView(AliveUi.body(this, "Create an Alive first, then its personality can be customized here."))
            empty.addView(AliveUi.gap(this, 10))
            empty.addView(AliveUi.button(this, "Create an Alive", true) {
                startActivity(Intent(this, AliveEditorActivity::class.java))
            })
            content.addView(AliveUi.card(this, empty))
        } else {
            profiles.forEach { profile ->
                val item = AliveUi.column(this)
                item.addView(AliveUi.heading(this, profile.name, 17f))
                item.addView(AliveUi.body(this, "Energy " + profile.energyLevel + "  •  Speed " +
                    profile.movementSpeedPercent + "%  •  Sleep " + profile.sleepFrequency, 12f))
                item.addView(AliveUi.gap(this, 8))
                item.addView(AliveUi.button(this, "Customize personality", true) {
                    startActivity(Intent(this, AliveEditorActivity::class.java)
                        .putExtra(AliveEditorActivity.EXTRA_ID, profile.id))
                })
                content.addView(AliveUi.card(this, item))
            }
        }
        addBack(content)
        setContentView(scroll)
    }

    private fun showSettings() {
        val (scroll, content) = AliveUi.scrollPage(this)
        rootView = content
        pageHeader(content, "SETTINGS", "Settings", "Android permissions used by S•S Alive.")

        val overlayGranted = Settings.canDrawOverlays(this)
        val overlay = AliveUi.column(this)
        overlay.addView(AliveUi.heading(this, "Display over other apps", 17f))
        overlay.addView(AliveUi.gap(this, 5))
        overlay.addView(AliveUi.body(this,
            if (overlayGranted) "Enabled — Alives can appear above other apps."
            else "Needed to display your Alives on the screen."))
        overlay.addView(AliveUi.gap(this, 12))
        overlay.addView(AliveUi.button(this,
            if (overlayGranted) "Manage permission" else "Allow display permission", true) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        content.addView(AliveUi.card(this, overlay))

        val accessibility = AliveUi.column(this)
        accessibility.addView(AliveUi.heading(this, "Home-screen detection", 17f))
        accessibility.addView(AliveUi.gap(this, 5))
        accessibility.addView(AliveUi.body(this,
            "Optional. Lets Alives detect visible home-screen icon areas so they can land on them. It does not read screen text or personal content."))
        accessibility.addView(AliveUi.gap(this, 12))
        accessibility.addView(AliveUi.button(this, "Open accessibility settings", false) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(AliveUi.card(this, accessibility))
        addBack(content)
        setContentView(scroll)
    }

    private fun aliveCard(profile: AliveProfile): android.view.View {
        val equipped = AliveRepository.isEquipped(this, profile.id)
        val card = AliveUi.column(this)
        val details = AliveUi.column(this)
        details.addView(AliveUi.heading(this, profile.name, 18f))
        details.addView(AliveUi.gap(this, 4))
        details.addView(AliveUi.body(this,
            if (equipped) "Equipped • Ready to appear" else "Saved • Not on screen", 12f))
        card.addView(details)
        card.addView(AliveUi.gap(this, 13))
        AliveUi.addButtonRow(this, card, listOf(
            AliveUi.button(this, if (equipped) "Unequip" else "Equip", true) {
                if (equipped) AliveRepository.unequip(this, profile.id)
                else AliveRepository.equip(this, profile)
                showAlives()
            },
            AliveUi.button(this, "Edit", false) {
                startActivity(Intent(this, AliveEditorActivity::class.java)
                    .putExtra(AliveEditorActivity.EXTRA_ID, profile.id))
            }
        ))
        card.addView(AliveUi.gap(this, 7))
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
                    showAlives()
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
                showAlives()
            }
            .show()
    }

    private fun refreshRunningAlives() {
        if (AliveService.isRunning && Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, AliveService::class.java)
                .setAction(AliveService.ACTION_SHOW_EQUIPPED))
        }
    }

    private fun refreshAccountConnection() {
        val started = AccountBridge.requestWithFeedback(this)
        if (started) {
            Toast.makeText(this, "Connecting to S•S Hub…", Toast.LENGTH_SHORT).show()
        } else {
            AccountBridge.consumeFeedback(this)
            Toast.makeText(
                this,
                "Could not open S•S Hub. Make sure it is installed and try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openHubForSignIn() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.ss.hub")
        if (launchIntent == null) {
            Toast.makeText(this, "S•S Hub is not installed.", Toast.LENGTH_LONG).show()
            return
        }
        startActivity(launchIntent)
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
