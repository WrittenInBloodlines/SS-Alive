package com.ss.alive.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class AliveEditorActivity : AppCompatActivity() {
    private lateinit var profile: AliveProfile
    private val stateRows = mutableMapOf<String, LinearLayout>()

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val state = pendingState ?: return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        profile.frames.getOrPut(state) { mutableListOf() }.add(uri.toString())
        refreshState(state)
        pendingState = null
    }

    private var pendingState: String? = null
    private lateinit var nameInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_ID)
        profile = if (id != null) {
            AliveRepository.get(this, id) ?: AliveProfile.empty(id, "Alive")
        } else {
            AliveProfile.empty("alive_${System.currentTimeMillis()}", "Alive 1")
        }
        buildEditor()
    }

    private fun buildEditor() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 48)
        }

        val title = TextView(this).apply {
            text = "Create Alive"
            textSize = 30f
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Build your own screen character with PNG animation frames."
            textSize = 16f
            setPadding(0, 12, 0, 24)
        }
        root.addView(subtitle)

        nameInput = EditText(this).apply {
            hint = "Alive name"
            setSingleLine(true)
            setText(profile.name)
        }
        root.addView(nameInput, matchParams())

        val scroll = ScrollView(this)
        val states = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(states)

        AliveProfile.STATES.forEach { state ->
            val section = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 20, 0, 8)
            }
            val heading = TextView(this).apply {
                text = state.titleCase()
                textSize = 20f
            }
            section.addView(heading)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            stateRows[state] = row
            section.addView(row)
            states.addView(section)
            refreshState(state)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val save = Button(this).apply {
            text = "SAVE"
            setOnClickListener { saveProfile() }
        }
        root.addView(save)

        val use = Button(this).apply {
            text = "USE ALIVE"
            setOnClickListener { saveProfile(useAfterSave = true) }
        }
        root.addView(use)

        setContentView(root)
    }

    private fun refreshState(state: String) {
        val row = stateRows[state] ?: return
        row.removeAllViews()
        val frames = profile.frameUris(state)
        val info = TextView(this).apply {
            text = if (frames.isEmpty()) "No PNG selected" else "${frames.size} frame${if (frames.size == 1) "" else "s"}"
            textSize = 15f
        }
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))

        val add = Button(this).apply {
            text = "+"
            setOnClickListener {
                pendingState = state
                pickImage.launch(arrayOf("image/png"))
            }
        }
        row.addView(add)

        if (frames.isNotEmpty()) {
            val clear = Button(this).apply {
                text = "CLEAR"
                setOnClickListener {
                    profile.frames[state]?.clear()
                    refreshState(state)
                }
            }
            row.addView(clear)
        }
    }

    private fun saveProfile(useAfterSave: Boolean = false) {
        profile.name = nameInput.text.toString().trim().ifEmpty { "Alive 1" }
        AliveRepository.save(this, profile)
        Toast.makeText(this, "Alive saved", Toast.LENGTH_SHORT).show()
        if (useAfterSave) {
            AliveRepository.setActive(this, profile)
            startService(Intent(this, AliveService::class.java))
            finish()
        }
    }

    private fun matchParams() = LinearLayout.LayoutParams(-1, -2)

    private fun String.titleCase(): String = lowercase().replaceFirstChar { it.uppercase() }

    companion object {
        const val EXTRA_ID = "alive_id"
    }
}
