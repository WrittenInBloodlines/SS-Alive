package com.ss.alive.alive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AliveEditorActivity : AppCompatActivity() {
    private lateinit var profile: AliveProfile
    private val stateRows = mutableMapOf<String, LinearLayout>()

    private val pickImageFromFiles = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> addSelectedImage(uri) }
    private val pickImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> addSelectedImage(uri) }
    private var pendingState: String? = null
    private lateinit var nameInput: EditText
    private lateinit var sizeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_ID)
        profile = if (id != null) AliveRepository.get(this, id) ?: AliveProfile.empty(id, "Alive")
        else AliveProfile.empty("alive_${System.currentTimeMillis()}", "Alive 1")
        buildEditor()
    }

    private fun buildEditor() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 48, 40, 48) }
        root.addView(TextView(this).apply { text = "Create Alive"; textSize = 30f })
        root.addView(TextView(this).apply {
            text = "Build your own screen character with PNG animation frames."
            textSize = 16f; setPadding(0, 12, 0, 20)
        })

        nameInput = EditText(this).apply { hint = "Alive name"; setSingleLine(true); setText(profile.name) }
        root.addView(nameInput, LinearLayout.LayoutParams(-1, -2))

        sizeLabel = TextView(this).apply { textSize = 17f; setPadding(0, 22, 0, 4) }
        root.addView(sizeLabel)
        val sizeBar = SeekBar(this).apply {
            max = 175 // 25%..200%
            progress = profile.sizePercent.coerceIn(25, 200) - 25
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    profile.sizePercent = progress + 25
                    updateSizeLabel()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        root.addView(sizeBar)
        updateSizeLabel()

        val scroll = ScrollView(this)
        val states = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(states)
        AliveProfile.STATES.forEach { state ->
            val section = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 20, 0, 8) }
            section.addView(TextView(this).apply { text = state.titleCase(); textSize = 20f })
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            stateRows[state] = row
            section.addView(row); states.addView(section); refreshState(state)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(Button(this).apply { text = "SAVE"; setOnClickListener { saveProfile() } })
        root.addView(Button(this).apply { text = "USE ALIVE"; setOnClickListener { saveProfile(true) } })
        setContentView(root)
    }

    private fun updateSizeLabel() { sizeLabel.text = "Size: ${profile.sizePercent}%" }

    private fun refreshState(state: String) {
        val row = stateRows[state] ?: return
        row.removeAllViews()
        val frames = profile.frameUris(state)
        row.addView(TextView(this).apply {
            text = if (frames.isEmpty()) "No PNG selected" else "${frames.size} frame${if (frames.size == 1) "" else "s"}"
            textSize = 15f
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "+"
            setOnClickListener { pendingState = state; showImageSourceDialog() }
        })
        if (frames.isNotEmpty()) row.addView(Button(this).apply {
            text = "CLEAR"
            setOnClickListener { profile.frames[state]?.clear(); refreshState(state) }
        })
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Choose image source")
            .setItems(arrayOf("Choose from Gallery", "Choose from Files")) { _, which ->
                if (which == 0) pickImageFromGallery.launch("image/png")
                else pickImageFromFiles.launch(arrayOf("image/png"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addSelectedImage(uri: Uri?) {
        if (uri == null) { pendingState = null; return }
        val state = pendingState ?: return
        if (uri.scheme != "content") { pendingState = null; return }
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        profile.frames.getOrPut(state) { mutableListOf() }.add(uri.toString())
        refreshState(state); pendingState = null
    }

    private fun saveProfile(useAfterSave: Boolean = false) {
        profile.name = nameInput.text.toString().trim().ifEmpty { "Alive 1" }
        profile.sizePercent = profile.sizePercent.coerceIn(25, 200)
        AliveRepository.save(this, profile)
        Toast.makeText(this, "Alive saved", Toast.LENGTH_SHORT).show()
        if (useAfterSave) {
            AliveRepository.setActive(this, profile)
            startService(Intent(this, AliveService::class.java))
            finish()
        }
    }

    private fun String.titleCase(): String = lowercase().replaceFirstChar { it.uppercase() }

    companion object { const val EXTRA_ID = "alive_id" }
}
