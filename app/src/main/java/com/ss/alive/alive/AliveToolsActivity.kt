package com.ss.alive.alive

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import android.widget.LinearLayout
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ss.alive.ui.AliveUi
import org.json.JSONObject

/**
 * Central place for tools that affect more than one editor screen.
 * Screen margins and statistics are stored separately from profiles so old
 * .ssalive packages remain compatible.
 */
class AliveToolsActivity : AppCompatActivity() {
    private val exportBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) runCatching {
            AlivePackageManager.exportAll(this, AliveRepository.all(this).filter { !it.isTemplate }, uri)
        }.onSuccess {
            Toast.makeText(this, "Complete Alive backup created.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, "Backup failed: ${it.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            AlivePackageManager.importAll(this, uri)
        }.onSuccess { profiles ->
            profiles.forEach { AliveRepository.save(this, it) }
            Toast.makeText(this, "${profiles.size} Alives restored.", Toast.LENGTH_LONG).show()
        }.onFailure {
            Toast.makeText(this, "Restore failed: ${it.message ?: "invalid backup"}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildPage()
    }

    private fun buildPage() {
        val (scroll, root) = AliveUi.scrollPage(this)
        root.addView(AliveUi.gap(this, 16))
        root.addView(AliveUi.label(this, "S•S ALIVE  /  ALIVE STUDIO"))
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.heading(this, "Alive Studio", 29f))
        root.addView(AliveUi.body(this, "Screen setup, behavior presets, outfits and activity statistics."))
        root.addView(AliveUi.gap(this, 20))

        addScreenSetup(root)
        addPresets(root)
        addRelationships(root)
        addOutfits(root)
        addBackups(root)
        addStatistics(root)
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.button(this, "Back to menu", false) { finish() })
        setContentView(scroll)
    }

    private fun addScreenSetup(root: LinearLayout) {
        val card = AliveUi.column(this)
        card.addView(AliveUi.heading(this, "Screen zones", 19f))
        card.addView(AliveUi.body(this, "Keep Alives away from parts of the screen, such as gesture areas or widgets. Values are saved on this device."))
        val prefs = getSharedPreferences(PREFS, 0)
        addZoneSlider(card, "Left reserved area", "left", prefs.getInt("left", 0))
        addZoneSlider(card, "Right reserved area", "right", prefs.getInt("right", 0))
        addZoneSlider(card, "Top reserved area", "top", prefs.getInt("top", 0))
        addZoneSlider(card, "Bottom reserved area", "bottom", prefs.getInt("bottom", 0))
        card.addView(CheckBox(this).apply {
            text = "Pause Alives while the screen is off"
            setTextColor(AliveUi.text)
            buttonTintList = android.content.res.ColorStateList.valueOf(AliveUi.blue)
            isChecked = prefs.getBoolean("pause_screen_off", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("pause_screen_off", checked).apply()
            }
        })
        card.addView(AliveUi.body(this, "Changes apply the next time equipped Alives are started. Keep the values modest so characters still have room to move.", 11f))
        root.addView(AliveUi.card(this, card))
    }

    private fun addZoneSlider(parent: LinearLayout, title: String, key: String, initial: Int) {
        val label = AliveUi.body(this, "$title  •  $initial%", 13f)
        parent.addView(label)
        val bar = SeekBar(this).apply {
            max = 35
            progress = initial.coerceIn(0, 35)
            progressTintList = android.content.res.ColorStateList.valueOf(AliveUi.blue)
            thumbTintList = android.content.res.ColorStateList.valueOf(AliveUi.blue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    label.text = "$title  •  $progress%"
                    getSharedPreferences(PREFS, 0).edit().putInt(key, progress).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        parent.addView(bar)
        parent.addView(AliveUi.gap(this, 5))
    }

    private fun addPresets(root: LinearLayout) {
        val card = AliveUi.column(this)
        card.addView(AliveUi.heading(this, "Personality presets", 19f))
        card.addView(AliveUi.body(this, "Apply a ready-made behavior profile to one of your Alives."))
        card.addView(AliveUi.gap(this, 8))
        card.addView(AliveUi.button(this, "Choose Alive and preset", true) {
            chooseAlive { profile -> choosePreset(profile) }
        })
        card.addView(AliveUi.gap(this, 6))
        card.addView(AliveUi.button(this, "Generate random personality", false) {
            chooseAlive { profile ->
                profile.energyLevel = kotlin.random.Random.nextInt(0, 101)
                profile.movementSpeedPercent = kotlin.random.Random.nextInt(50, 151)
                profile.sleepFrequency = kotlin.random.Random.nextInt(0, 101)
                profile.curiosity = kotlin.random.Random.nextInt(0, 101)
                profile.gravityStrengthPercent = kotlin.random.Random.nextInt(50, 151)
                profile.interactionFrequency = kotlin.random.Random.nextInt(0, 101)
                AliveRepository.save(this, profile)
                Toast.makeText(this, "Random personality created for ${profile.name}.", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(AliveUi.card(this, card))
    }

    private fun choosePreset(profile: AliveProfile) {
        val names = arrayOf("Balanced", "Energetic", "Sleepy", "Curious", "Calm and floaty", "Social")
        AlertDialog.Builder(this)
            .setTitle("Preset for ${profile.name}")
            .setItems(names) { _, index ->
                when (index) {
                    0 -> { profile.energyLevel = 50; profile.movementSpeedPercent = 100; profile.sleepFrequency = 35; profile.curiosity = 50; profile.gravityStrengthPercent = 100; profile.interactionFrequency = 50 }
                    1 -> { profile.energyLevel = 90; profile.movementSpeedPercent = 135; profile.sleepFrequency = 10; profile.curiosity = 75; profile.gravityStrengthPercent = 110; profile.interactionFrequency = 70 }
                    2 -> { profile.energyLevel = 25; profile.movementSpeedPercent = 75; profile.sleepFrequency = 90; profile.curiosity = 20; profile.gravityStrengthPercent = 100; profile.interactionFrequency = 20 }
                    3 -> { profile.energyLevel = 65; profile.movementSpeedPercent = 110; profile.sleepFrequency = 30; profile.curiosity = 100; profile.gravityStrengthPercent = 95; profile.interactionFrequency = 60 }
                    4 -> { profile.energyLevel = 40; profile.movementSpeedPercent = 85; profile.sleepFrequency = 65; profile.curiosity = 35; profile.gravityStrengthPercent = 55; profile.interactionFrequency = 35 }
                    5 -> { profile.energyLevel = 70; profile.movementSpeedPercent = 105; profile.sleepFrequency = 25; profile.curiosity = 65; profile.gravityStrengthPercent = 100; profile.interactionFrequency = 100; profile.interactionEnabled = true }
                }
                AliveRepository.save(this, profile)
                Toast.makeText(this, "${names[index]} preset applied to ${profile.name}.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addRelationships(root: LinearLayout) {
        val card = AliveUi.column(this)
        card.addView(AliveUi.heading(this, "Alive relationships", 19f))
        card.addView(AliveUi.body(this, "Set how two Alives know each other. Unknown pairs will not start automatic interactions."))
        card.addView(AliveUi.gap(this, 8))
        card.addView(AliveUi.button(this, "Set relationship", false) { chooseRelationshipPair() })
        root.addView(AliveUi.card(this, card))
    }

    private fun chooseRelationshipPair() {
        val profiles = AliveRepository.all(this).filter { !it.isTemplate }
        if (profiles.size < 2) {
            Toast.makeText(this, "Create at least two Alives first.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Choose first Alive")
            .setItems(profiles.map { it.name }.toTypedArray()) { _, firstIndex ->
                val first = profiles[firstIndex]
                val others = profiles.filter { it.id != first.id }
                AlertDialog.Builder(this)
                    .setTitle("Choose second Alive")
                    .setItems(others.map { it.name }.toTypedArray()) { _, secondIndex ->
                        chooseRelationshipLevel(first, others[secondIndex])
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseRelationshipLevel(first: AliveProfile, second: AliveProfile) {
        val labels = arrayOf("Unknown", "Acquaintance", "Friend", "Close", "Special", "Custom")
        AlertDialog.Builder(this)
            .setTitle("${first.name} & ${second.name}")
            .setItems(labels) { _, index ->
                if (index == labels.lastIndex) {
                    val input = android.widget.EditText(this).apply { hint = "Relationship name"; setSingleLine(true) }
                    AlertDialog.Builder(this)
                        .setTitle("Custom relationship")
                        .setView(input)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save") { _, _ ->
                            val value = input.text.toString().trim().take(24)
                            if (value.isNotBlank()) AliveRelationshipStore.save(this, first.id, second.id, "Custom: $value")
                        }
                        .show()
                } else {
                    AliveRelationshipStore.save(this, first.id, second.id, labels[index])
                    Toast.makeText(this, "Relationship updated.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addOutfits(root: LinearLayout) {
        val card = AliveUi.column(this)
        card.addView(AliveUi.heading(this, "Outfit slots", 19f))
        card.addView(AliveUi.body(this, "Save and restore an Alive's current animation set as an outfit. This keeps the same character and behavior settings."))
        card.addView(AliveUi.gap(this, 8))
        card.addView(AliveUi.button(this, "Manage outfits", false) {
            chooseAlive { profile -> manageOutfits(profile) }
        })
        root.addView(AliveUi.card(this, card))
    }

    private fun manageOutfits(profile: AliveProfile) {
        val prefs = getSharedPreferences(PREFS, 0)
        val key = "outfits_${profile.id}"
        val saved = runCatching { JSONObject(prefs.getString(key, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val labels = (0 until saved.length()).map { saved.names()?.optString(it).orEmpty() }.filter { it.isNotBlank() }
        val actions = mutableListOf("Save current look")
        actions.addAll(labels.map { "Wear: $it" })
        AlertDialog.Builder(this)
            .setTitle("${profile.name} outfits")
            .setItems(actions.toTypedArray()) { _, index ->
                if (index == 0) askOutfitName(profile, saved, key)
                else {
                    val name = labels[index - 1]
                    val frames = saved.optJSONObject(name) ?: return@setItems
                    profile.frames.clear()
                    AliveProfile.STATES.forEach { state ->
                        val array = frames.optJSONArray(state) ?: return@forEach
                        val list = mutableListOf<String>()
                        for (i in 0 until array.length()) list += array.optString(i)
                        profile.frames[state] = list
                    }
                    AliveRepository.save(this, profile)
                    Toast.makeText(this, "Outfit $name restored. Restart the Alive to refresh its sprite cache.", Toast.LENGTH_LONG).show()
                }
            }
            .setNeutralButton("Delete outfit") { _, _ -> chooseOutfitToDelete(saved, key) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun askOutfitName(profile: AliveProfile, saved: JSONObject, key: String) {
        val input = android.widget.EditText(this).apply { hint = "Outfit name"; setSingleLine(true) }
        AlertDialog.Builder(this)
            .setTitle("Save current look")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim().take(32)
                if (name.isBlank()) {
                    Toast.makeText(this, "Enter an outfit name.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val frames = JSONObject()
                AliveProfile.STATES.forEach { state ->
                    val array = org.json.JSONArray()
                    profile.frameUris(state).forEach(array::put)
                    frames.put(state, array)
                }
                saved.put(name, frames)
                getSharedPreferences(PREFS, 0).edit().putString(key, saved.toString()).apply()
                Toast.makeText(this, "Outfit saved.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun chooseOutfitToDelete(saved: JSONObject, key: String) {
        val names = (0 until saved.length()).map { saved.names()?.optString(it).orEmpty() }.filter { it.isNotBlank() }
        if (names.isEmpty()) {
            Toast.makeText(this, "No saved outfits.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete outfit")
            .setItems(names.toTypedArray()) { _, index ->
                saved.remove(names[index])
                getSharedPreferences(PREFS, 0).edit().putString(key, saved.toString()).apply()
                Toast.makeText(this, "Outfit deleted.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addBackups(root: LinearLayout) {
        val card = AliveUi.column(this)
        card.addView(AliveUi.heading(this, "Backup & restore", 19f))
        card.addView(AliveUi.body(this, "Create one ZIP backup containing all your custom Alives, animation frames, sounds and settings, or restore them together."))
        card.addView(AliveUi.gap(this, 8))
        AliveUi.addButtonRow(this, card, listOf(
            AliveUi.button(this, "Create backup", true) { exportBackup.launch("ss-alive-backup.zip") },
            AliveUi.button(this, "Restore backup", false) { importBackup.launch(arrayOf("application/zip", "application/octet-stream")) }
        ))
        root.addView(AliveUi.card(this, card))
    }

    private fun addStatistics(root: LinearLayout) {
        val card = AliveUi.column(this)
        card.addView(AliveUi.heading(this, "Activity statistics", 19f))
        card.addView(AliveUi.body(this, "See how often each Alive has been tapped or landed on a home-screen surface."))
        card.addView(AliveUi.gap(this, 8))
        val profiles = AliveRepository.all(this).filter { !it.isTemplate }
        if (profiles.isEmpty()) {
            card.addView(AliveUi.body(this, "Create an Alive to start collecting statistics."))
        } else {
            profiles.forEach { profile ->
                val stats = AliveStatistics.read(this, profile.id)
                card.addView(AliveUi.heading(this, profile.name, 16f))
                card.addView(AliveUi.body(this, "Taps: ${stats.first}   •   Landings: ${stats.second}   •   Active: ${stats.third / 60000L} min", 12f))
                card.addView(AliveUi.button(this, "Reset ${profile.name} statistics", false) {
                    AliveStatistics.reset(this, profile.id)
                    Toast.makeText(this, "Statistics reset. Reopen Alive Studio to refresh.", Toast.LENGTH_SHORT).show()
                })
                card.addView(AliveUi.gap(this, 8))
            }
        }
        root.addView(AliveUi.card(this, card))
    }

    private fun chooseAlive(onChosen: (AliveProfile) -> Unit) {
        val profiles = AliveRepository.all(this).filter { !it.isTemplate }
        if (profiles.isEmpty()) {
            Toast.makeText(this, "Create an Alive first.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Choose an Alive")
            .setItems(profiles.map { it.name }.toTypedArray()) { _, index ->
                onChosen(profiles[index])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val PREFS = "ss_alive_tools"
    }
}

object AliveStatistics {
    private fun prefs(context: android.content.Context) =
        context.getSharedPreferences("ss_alive_statistics", 0)

    fun recordTap(context: android.content.Context, id: String) {
        val p = prefs(context)
        p.edit().putInt("${id}_taps", p.getInt("${id}_taps", 0) + 1).apply()
    }

    fun recordLanding(context: android.content.Context, id: String) {
        val p = prefs(context)
        p.edit().putInt("${id}_landings", p.getInt("${id}_landings", 0) + 1).apply()
    }

    fun recordActive(context: android.content.Context, id: String, durationMs: Long) {
        if (durationMs <= 0L) return
        val p = prefs(context)
        p.edit().putLong("${id}_active_ms", p.getLong("${id}_active_ms", 0L) + durationMs).apply()
    }

    fun read(context: android.content.Context, id: String): Triple<Int, Int, Long> {
        val p = prefs(context)
        return Triple(
            p.getInt("${id}_taps", 0),
            p.getInt("${id}_landings", 0),
            p.getLong("${id}_active_ms", 0L)
        )
    }

    fun reset(context: android.content.Context, id: String) {
        prefs(context).edit().remove("${id}_taps").remove("${id}_landings").remove("${id}_active_ms").apply()
    }
}


object AlivePositionStore {
    private const val PREFS = "ss_alive_positions"

    private fun orientation(context: android.content.Context): String {
        val metrics = context.resources.displayMetrics
        return if (metrics.heightPixels >= metrics.widthPixels) "portrait" else "landscape"
    }

    fun save(context: android.content.Context, id: String, x: Int, y: Int) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putInt("${id}_${orientation(context)}_x", x)
            .putInt("${id}_${orientation(context)}_y", y)
            .apply()
    }

    fun load(context: android.content.Context, id: String): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val suffix = orientation(context)
        val xKey = "${id}_${suffix}_x"
        val yKey = "${id}_${suffix}_y"
        if (!prefs.contains(xKey) || !prefs.contains(yKey)) return null
        return prefs.getInt(xKey, 0) to prefs.getInt(yKey, 0)
    }
}


object AliveRelationshipStore {
    private const val PREFS = "ss_alive_relationships"

    private fun key(first: String, second: String): String =
        listOf(first, second).sorted().joinToString(":")

    fun save(context: android.content.Context, first: String, second: String, level: String) {
        context.getSharedPreferences(PREFS, 0).edit()
            .putString(key(first, second), level.take(40))
            .apply()
    }

    fun level(context: android.content.Context, first: String, second: String): String =
        context.getSharedPreferences(PREFS, 0).getString(key(first, second), "Acquaintance") ?: "Acquaintance"
}
