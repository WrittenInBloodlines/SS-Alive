package com.ss.alive.alive

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
    private lateinit var preview: AnimatedFramePreview
    private lateinit var previewStateLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_ID)
        profile = if (id != null) AliveRepository.get(this, id) ?: AliveProfile.empty(id, "Alive")
        else AliveProfile.empty("alive_${System.currentTimeMillis()}", "Alive 1")
        buildEditor()
    }

    override fun onPause() {
        super.onPause()
        if (::preview.isInitialized) preview.stop()
    }

    private fun buildEditor() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 48)
        }
        root.addView(TextView(this).apply {
            text = "CREATE ALIVE"
            textSize = 30f
        })
        root.addView(TextView(this).apply {
            text = "Make the character yourself: add your own PNG frames, choose how fast every animation plays, and preview it before saving."
            textSize = 16f
            setPadding(0, 12, 0, 16)
        })

        nameInput = EditText(this).apply {
            hint = "Alive name"
            setSingleLine(true)
            setText(profile.name)
        }
        root.addView(nameInput, LinearLayout.LayoutParams(-1, -2))

        sizeLabel = TextView(this).apply { textSize = 17f; setPadding(0, 18, 0, 4) }
        root.addView(sizeLabel)
        val sizeBar = SeekBar(this).apply {
            max = 175
            progress = profile.sizePercent.coerceIn(25, 200) - 25
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    profile.sizePercent = p + 25
                    updateSizeLabel()
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        root.addView(sizeBar)
        updateSizeLabel()

        previewStateLabel = TextView(this).apply {
            text = "PREVIEW • IDLE"
            textSize = 18f
            setPadding(0, 22, 0, 8)
        }
        root.addView(previewStateLabel)

        preview = AnimatedFramePreview(this)
        root.addView(preview, LinearLayout.LayoutParams(-1, 300))

        val scroll = ScrollView(this)
        val states = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(states)
        AliveProfile.STATES.forEach { state ->
            val section = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 20, 0, 8)
            }
            section.addView(TextView(this).apply {
                text = state.titleCase()
                textSize = 20f
            })
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

        root.addView(Button(this).apply {
            text = "SAVE"
            setOnClickListener { saveProfile() }
        })
        root.addView(Button(this).apply {
            text = "USE ALIVE"
            setOnClickListener { saveProfile(true) }
        })
        if (!profile.isTemplate) root.addView(Button(this).apply {
            text = "DELETE ALIVE"
            setOnClickListener { confirmDelete() }
        })
        setContentView(root)
    }

    private fun updateSizeLabel() {
        sizeLabel.text = "Size: ${profile.sizePercent}%"
    }

    private fun refreshState(state: String) {
        val row = stateRows[state] ?: return
        row.removeAllViews()
        val frames = profile.frameUris(state)
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(TextView(this).apply {
            text = if (frames.isEmpty()) "No PNG selected" else "${frames.size} frame${if (frames.size == 1) "" else "s"}"
            textSize = 15f
        })
        val speedLabel = TextView(this).apply {
            text = "Speed: ${profile.speedFps(state)} FPS"
            textSize = 13f
        }
        info.addView(speedLabel)
        val speedBar = SeekBar(this).apply {
            max = 29
            progress = profile.speedFps(state) - 1
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    profile.setSpeedFps(state, p + 1)
                    speedLabel.text = "Speed: ${profile.speedFps(state)} FPS"
                    if (preview.currentState == state) preview.restart(profile.frameUris(state), profile.speedFps(state), state)
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        info.addView(speedBar)
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "+"
            setOnClickListener {
                pendingState = state
                showImageSourceDialog()
            }
        })
        row.addView(Button(this).apply {
            text = "PREVIEW"
            isEnabled = frames.isNotEmpty()
            setOnClickListener { startPreview(state) }
        })
        if (frames.isNotEmpty()) row.addView(Button(this).apply {
            text = "CLEAR"
            setOnClickListener {
                profile.frames[state]?.clear()
                refreshState(state)
                if (preview.currentState == state) preview.stop()
            }
        })
    }

    private fun startPreview(state: String) {
        val frames = profile.frameUris(state)
        if (frames.isEmpty()) return
        previewStateLabel.text = "PREVIEW • ${state.titleCase()} • ${profile.speedFps(state)} FPS"
        preview.restart(frames, profile.speedFps(state), state)
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Add animation frame")
            .setItems(arrayOf("Choose from Gallery", "Choose from Files")) { _, which ->
                if (which == 0) pickImageFromGallery.launch("image/png")
                else pickImageFromFiles.launch(arrayOf("image/png"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addSelectedImage(uri: Uri?) {
        if (uri == null) {
            pendingState = null
            return
        }
        val state = pendingState ?: return
        if (uri.scheme != "content") {
            pendingState = null
            return
        }
        runCatching {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        profile.frames.getOrPut(state) { mutableListOf() }.add(uri.toString())
        refreshState(state)
        pendingState = null
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

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Alive?")
            .setMessage("This will permanently remove ${profile.name} and its animation frames from S•S Alive.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                AliveRepository.delete(this, profile.id)
                Toast.makeText(this, "Alive deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    private fun String.titleCase(): String = lowercase().replaceFirstChar { it.uppercase() }

    companion object { const val EXTRA_ID = "alive_id" }
}

private class AnimatedFramePreview(context: android.content.Context) : ImageView(context) {
    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<String> = emptyList()
    private var fps: Int = AliveProfile.DEFAULT_FPS
    private var index = 0
    var currentState: String? = null
        private set

    init {
        scaleType = ScaleType.CENTER_INSIDE
        adjustViewBounds = true
    }

    fun restart(newFrames: List<String>, newFps: Int, state: String? = currentState) {
        stop()
        frames = newFrames.toList()
        fps = newFps.coerceIn(1, 30)
        index = 0
        currentState = state
        showCurrentFrame()
        if (frames.size > 1) handler.postDelayed(tick, frameDelay())
    }

    fun stop() {
        handler.removeCallbacks(tick)
    }

    private fun frameDelay(): Long = (1000L / fps.coerceAtLeast(1)).coerceAtLeast(16L)

    private val tick = object : Runnable {
        override fun run() {
            if (frames.isEmpty()) return
            index = (index + 1) % frames.size
            showCurrentFrame()
            handler.postDelayed(this, frameDelay())
        }
    }

    private fun showCurrentFrame() {
        if (frames.isEmpty()) {
            setImageDrawable(null)
            return
        }
        val uri = Uri.parse(frames[index])
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        setImageBitmap(bitmap)
    }
}
