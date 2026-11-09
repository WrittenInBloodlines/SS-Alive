package com.ss.alive.alive

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.media.MediaPlayer
import android.widget.Button
import android.widget.CheckBox
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
import androidx.core.content.ContextCompat
import com.ss.alive.ui.AliveUi

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
    private lateinit var soundStatus: TextView
    private val exportPackage = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) runCatching { AlivePackageManager.export(this, profile, uri) }
            .onSuccess { Toast.makeText(this, "Alive package exported.", Toast.LENGTH_LONG).show() }
            .onFailure { Toast.makeText(this, "Export failed: " + (it.message ?: "unknown error"), Toast.LENGTH_LONG).show() }
    }
    private val importPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching { AlivePackageManager.import(this, uri) }
            .onSuccess { imported ->
                AliveRepository.save(this, imported)
                Toast.makeText(this, imported.name + " imported.", Toast.LENGTH_LONG).show()
                finish()
            }
            .onFailure { Toast.makeText(this, "Import failed: " + (it.message ?: "invalid package"), Toast.LENGTH_LONG).show() }
    }
    private val pickSound = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            profile.soundUri = uri.toString()
            if (::soundStatus.isInitialized) soundStatus.text = "Sound selected"
        }
    }

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
        if (::preview.isInitialized) preview.stop()
        val (scroll, root) = AliveUi.scrollPage(this)
        val isEditing = intent.getStringExtra(EXTRA_ID) != null

        root.addView(AliveUi.label(this, "S•S ALIVE  /  CHARACTER STUDIO"))
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.heading(this, if (isEditing) "Edit your Alive" else "Create an Alive", 28f))
        root.addView(AliveUi.body(this,
            "Build your character, choose its size, and add the animations it can use on your screen."))
        root.addView(AliveUi.gap(this, 20))

        val character = AliveUi.column(this)
        character.addView(AliveUi.heading(this, "Character", 19f))
        character.addView(AliveUi.body(this, "Give your Alive a name and choose how large it appears."))
        character.addView(AliveUi.gap(this, 12))
        nameInput = EditText(this).apply {
            hint = "Enter a character name"
            setSingleLine(true)
            setText(profile.name)
            setTextColor(AliveUi.text)
            setHintTextColor(AliveUi.muted)
            backgroundTintList = ColorStateList.valueOf(AliveUi.blue)
            textSize = 16f
        }
        character.addView(nameInput, LinearLayout.LayoutParams(-1, -2))
        character.addView(AliveUi.gap(this, 14))
        sizeLabel = AliveUi.body(this, "", 14f)
        character.addView(sizeLabel)
        val sizeBar = SeekBar(this).apply {
            max = 175
            progress = profile.sizePercent.coerceIn(25, 200) - 25
            progressTintList = ColorStateList.valueOf(AliveUi.blue)
            thumbTintList = ColorStateList.valueOf(AliveUi.blue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    profile.sizePercent = p + 25
                    updateSizeLabel()
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        character.addView(sizeBar)
        updateSizeLabel()
        root.addView(AliveUi.card(this, character))

        val previewCard = AliveUi.column(this)
        previewCard.addView(AliveUi.heading(this, "Animation preview", 19f))
        previewStateLabel = AliveUi.body(this, "Choose an animation below to preview it.", 13f)
        previewCard.addView(previewStateLabel)
        previewCard.addView(AliveUi.gap(this, 8))
        preview = AnimatedFramePreview(this).apply {
            setBackgroundColor(AliveUi.surfaceRaised)
            setPadding(AliveUi.dp(this@AliveEditorActivity, 12), AliveUi.dp(this@AliveEditorActivity, 12), AliveUi.dp(this@AliveEditorActivity, 12), AliveUi.dp(this@AliveEditorActivity, 12))
        }
        previewCard.addView(preview, LinearLayout.LayoutParams(-1, AliveUi.dp(this, 220)))
        root.addView(AliveUi.card(this, previewCard))

        root.addView(AliveUi.heading(this, "Animations", 22f))
        root.addView(AliveUi.body(this,
            "Add PNG frames for each movement. You can preview each animation before saving."))
        root.addView(AliveUi.gap(this, 12))

        AliveProfile.STATES.forEach { state ->
            val section = AliveUi.column(this)
            val title = when (state) {
                AliveProfile.INTERACTION -> "Interaction"
                AliveProfile.APP_SIT -> "Home-screen landing"
                else -> state.titleCase()
            }
            section.addView(AliveUi.heading(this, title, 17f))
            val description = when (state) {
                AliveProfile.INTERACTION -> "Played when this Alive interacts with another."
                AliveProfile.APP_SIT -> "Played when this Alive lands on an app icon on the home screen."
                else -> "Frames used for the ${state.titleCase().lowercase()} animation."
            }
            section.addView(AliveUi.body(this, description, 12f))
            section.addView(AliveUi.gap(this, 8))
            val row = AliveUi.column(this)
            stateRows[state] = row
            section.addView(row)
            root.addView(AliveUi.card(this, section))
            refreshState(state)
        }

        val optionalSettings = AliveUi.column(this)
        optionalSettings.addView(AliveUi.heading(this, "Optional settings", 19f))
        optionalSettings.addView(AliveUi.body(this, "Advanced options stay hidden until you open a section.", 12f))
        optionalSettings.addView(AliveUi.gap(this, 10))

        val behavior = AliveUi.column(this)
        behavior.addView(CheckBox(this).apply {
            text = "Allow interactions with other Alives"
            setTextColor(AliveUi.text)
            buttonTintList = ColorStateList.valueOf(AliveUi.blue)
            isChecked = profile.interactionEnabled
            setPadding(0, AliveUi.dp(this@AliveEditorActivity, 8), 0, 4)
            setOnCheckedChangeListener { _, checked -> profile.interactionEnabled = checked }
        })
        behavior.addView(AliveUi.body(this, "Both characters need this enabled and an Interaction animation.", 12f))
        behavior.addView(CheckBox(this).apply {
            text = "Enable Ghost Mode"
            setTextColor(AliveUi.text)
            buttonTintList = ColorStateList.valueOf(AliveUi.blue)
            isChecked = profile.ghostMode
            setPadding(0, AliveUi.dp(this@AliveEditorActivity, 8), 0, 4)
            setOnCheckedChangeListener { _, checked -> profile.ghostMode = checked }
        })
        behavior.addView(AliveUi.body(this, "Makes this Alive partly transparent and lets taps pass through it.", 12f))
        optionalSettings.addView(collapsibleSection("Behavior & Ghost Mode", behavior))

        val touchReactions = AliveUi.column(this)
        touchReactions.addView(AliveUi.body(this, "Choose what happens when the head or body is tapped.", 12f))
        touchReactions.addView(AliveUi.gap(this, 6))
        touchReactions.addView(reactionButton("Head reaction", profile.headTapReaction) { profile.headTapReaction = it })
        touchReactions.addView(reactionButton("Body reaction", profile.bodyTapReaction) { profile.bodyTapReaction = it })
        optionalSettings.addView(collapsibleSection("Touch reactions", touchReactions))

        val props = AliveUi.column(this)
        props.addView(AliveUi.body(this, "Choose a small prop that appears beside your Alive while it moves.", 12f))
        props.addView(AliveUi.gap(this, 6))
        props.addView(AliveUi.button(this, "Choose carried item", false) {
            val items = arrayOf("📖 Book", "🌹 Flower", "🔑 Key", "☕ Coffee", "⭐ Star", "🎁 Gift", "No item")
            val values = arrayOf("📖", "🌹", "🔑", "☕", "⭐", "🎁", "")
            AlertDialog.Builder(this)
                .setTitle("Carried item")
                .setItems(items) { _, index ->
                    profile.heldItem = values[index].takeIf { it.isNotBlank() }
                    buildEditor()
                }
                .setNegativeButton("Cancel", null)
                .show()
        })
        props.addView(AliveUi.body(this, "Current item: ${profile.heldItem ?: "None"}", 12f))
        optionalSettings.addView(collapsibleSection("Props & Items", props))

        val personality = AliveUi.column(this)
        personality.addView(AliveUi.body(this, "These values affect movement, resting and interaction timing.", 12f))
        personality.addView(AliveUi.button(this, "Mood  •  ${profile.mood.lowercase().replaceFirstChar { it.uppercase() }}", false) {
            val moods = arrayOf("Neutral", "Cheerful", "Sleepy", "Curious", "Calm", "Energetic")
            val values = arrayOf("NEUTRAL", "CHEERFUL", "SLEEPY", "CURIOUS", "CALM", "ENERGETIC")
            AlertDialog.Builder(this)
                .setTitle("Choose a mood")
                .setItems(moods) { _, index ->
                    profile.mood = values[index]
                    buildEditor()
                }
                .setNegativeButton("Cancel", null)
                .show()
        })
        addSettingSlider(personality, "Energy", "Higher energy means more active movement.", profile.energyLevel, 0, 100) { profile.energyLevel = it }
        addSettingSlider(personality, "Movement speed", "Controls walking speed.", profile.movementSpeedPercent, 50, 150, "%") { profile.movementSpeedPercent = it }
        addSettingSlider(personality, "Sleep frequency", "Controls how often and how long this Alive rests.", profile.sleepFrequency, 0, 100) { profile.sleepFrequency = it }
        addSettingSlider(personality, "Curiosity", "Higher curiosity makes direction changes more likely.", profile.curiosity, 0, 100) { profile.curiosity = it }
        addSettingSlider(personality, "Gravity", "Lower gravity makes falling and jumping feel floatier.", profile.gravityStrengthPercent, 50, 150, "%") { profile.gravityStrengthPercent = it }
        addSettingSlider(personality, "Interaction frequency", "Controls the cooldown before another interaction.", profile.interactionFrequency, 0, 100) { profile.interactionFrequency = it }
        optionalSettings.addView(collapsibleSection("Personality", personality))

        val sounds = AliveUi.column(this)
        sounds.addView(CheckBox(this).apply {
            text = "Enable sound effects"
            setTextColor(AliveUi.text)
            buttonTintList = ColorStateList.valueOf(AliveUi.blue)
            isChecked = profile.soundsEnabled
            setOnCheckedChangeListener { _, checked -> profile.soundsEnabled = checked }
        })
        soundStatus = AliveUi.body(this, if (profile.soundUri.isNullOrBlank()) "No sound selected" else "Sound selected", 12f)
        sounds.addView(soundStatus)
        sounds.addView(AliveUi.button(this, "Choose sound file", false) { pickSound.launch(arrayOf("audio/*")) })
        addSettingSlider(sounds, "Sound volume", "Volume for tap and landing effects.", profile.soundVolume, 0, 100, "%") { profile.soundVolume = it }
        sounds.addView(AliveUi.button(this, "Preview sound", false) {
            val uri = profile.soundUri
            if (uri.isNullOrBlank()) {
                Toast.makeText(this, "Choose a sound file first.", Toast.LENGTH_SHORT).show()
            } else {
                runCatching {
                    MediaPlayer.create(this, Uri.parse(uri))?.also { player ->
                        val volume = profile.soundVolume / 100f
                        player.setVolume(volume, volume)
                        player.setOnCompletionListener { it.release() }
                        player.start()
                    }
                }.onFailure { Toast.makeText(this, "Could not play this sound.", Toast.LENGTH_SHORT).show() }
            }
        })
        optionalSettings.addView(collapsibleSection("Sounds", sounds))

        val sharing = AliveUi.column(this)
        sharing.addView(AliveUi.body(this, "Packages include profile settings, animation PNGs and the selected sound.", 12f))
        sharing.addView(AliveUi.gap(this, 8))
        AliveUi.addButtonRow(this, sharing, listOf(
            AliveUi.button(this, "Export package", true) {
                profile.name = nameInput.text.toString().trim().ifEmpty { "Alive 1" }
                val safeName = profile.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
                exportPackage.launch(safeName + ".ssalive")
            },
            AliveUi.button(this, "Import package", false) {
                importPackage.launch(arrayOf("application/zip", "application/octet-stream"))
            }
        ))
        optionalSettings.addView(collapsibleSection("Import & Export", sharing))
        root.addView(AliveUi.card(this, optionalSettings))

        root.addView(AliveUi.gap(this, 4))
        AliveUi.addButtonRow(this, root, listOf(
            AliveUi.button(this, "Save", true) { saveProfile() },
            AliveUi.button(this, "Save & use", false) { saveProfile(true) }
        ))
        if (!profile.isTemplate) {
            root.addView(AliveUi.gap(this, 8))
            root.addView(AliveUi.button(this, "Delete this Alive", false) { confirmDelete() })
        }
        root.addView(AliveUi.gap(this, 20))
        setContentView(scroll)
    }

    private fun collapsibleSection(title: String, section: LinearLayout): LinearLayout {
        val wrapper = AliveUi.column(this)
        section.visibility = View.GONE
        val header = AliveUi.button(this, title + "  +", false) { }
        header.setOnClickListener {
            val expanded = section.visibility != View.VISIBLE
            section.visibility = if (expanded) View.VISIBLE else View.GONE
            header.text = title + if (expanded) "  −" else "  +"
        }
        wrapper.addView(header)
        wrapper.addView(section)
        wrapper.addView(AliveUi.gap(this, 7))
        return wrapper
    }

    private fun addSettingSlider(
        parent: LinearLayout,
        title: String,
        description: String,
        initial: Int,
        minimum: Int,
        maximum: Int,
        suffix: String = "",
        onChanged: (Int) -> Unit
    ) {
        val valueLabel = AliveUi.body(this, title + "  •  " + initial + suffix, 14f)
        parent.addView(valueLabel)
        parent.addView(AliveUi.body(this, description, 11f))
        val slider = SeekBar(this).apply {
            max = maximum - minimum
            progress = (initial - minimum).coerceIn(0, max)
            progressTintList = ColorStateList.valueOf(AliveUi.blue)
            thumbTintList = ColorStateList.valueOf(AliveUi.blue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + minimum
                    valueLabel.text = title + "  •  " + value + suffix
                    onChanged(value)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        parent.addView(slider)
        parent.addView(AliveUi.gap(this, 8))
    }

    private fun updateSizeLabel() {
        sizeLabel.text = "Character size  •  ${profile.sizePercent}%"
    }

    private fun refreshState(state: String) {
        val container = stateRows[state] ?: return
        container.removeAllViews()
        val frames = profile.frameUris(state)
        val info = AliveUi.row(this)
        val frameInfo = AliveUi.column(this)
        frameInfo.addView(AliveUi.heading(this,
            if (frames.isEmpty()) "No frames yet" else "${frames.size} frame${if (frames.size == 1) "" else "s"}", 15f))
        val speedLabel = AliveUi.body(this, "Playback speed  •  ${profile.speedFps(state)} FPS", 12f)
        frameInfo.addView(speedLabel)
        info.addView(frameInfo, LinearLayout.LayoutParams(0, -2, 1f))
        container.addView(info)

        val speedBar = SeekBar(this).apply {
            max = 29
            progress = profile.speedFps(state) - 1
            progressTintList = ColorStateList.valueOf(AliveUi.blue)
            thumbTintList = ColorStateList.valueOf(AliveUi.blue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    profile.setSpeedFps(state, p + 1)
                    speedLabel.text = "Playback speed  •  ${profile.speedFps(state)} FPS"
                    if (preview.currentState == state) preview.restart(profile.frameUris(state), profile.speedFps(state), state)
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        container.addView(speedBar)
        container.addView(AliveUi.gap(this, 6))
        val actions = mutableListOf(
            AliveUi.button(this, "Add PNG frames", true) {
                pendingState = state
                showImageSourceDialog()
            },
            AliveUi.button(this, "Preview", false) { startPreview(state) }
        )
        actions[1].isEnabled = frames.isNotEmpty()
        AliveUi.addButtonRow(this, container, actions)
        if (frames.isNotEmpty()) {
            container.addView(AliveUi.gap(this, 6))
            container.addView(AliveUi.button(this, "Manage frame order", false) { manageFrames(state) })
            container.addView(AliveUi.gap(this, 6))
            container.addView(AliveUi.button(this, "Remove all frames", false) {
                AlertDialog.Builder(this)
                    .setTitle("Remove animation frames?")
                    .setMessage("This clears all frames for this animation.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove") { _, _ ->
                        profile.frames[state]?.clear()
                        refreshState(state)
                        if (preview.currentState == state) preview.stop()
                    }
                    .show()
            })
        }
    }

    private fun reactionButton(title: String, current: String, onSelected: (String) -> Unit): Button =
        AliveUi.button(this, "$title  •  $current", false) {
            val choices = arrayOf("Turn around", "Jump", "Sit briefly")
            val values = arrayOf("TURN", "JUMP", "SIT")
            AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(choices) { _, index ->
                    onSelected(values[index])
                    // Rebuild the editor so the selected value is visible.
                    buildEditor()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

    private fun manageFrames(state: String) {
        val frames = profile.frames.getOrPut(state) { mutableListOf() }
        if (frames.isEmpty()) return
        val labels = frames.indices.map { "Frame ${it + 1}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Manage ${state.titleCase()} frames")
            .setItems(labels) { _, selected ->
                val actions = arrayOf("Move earlier", "Move later", "Remove frame")
                AlertDialog.Builder(this)
                    .setTitle(labels[selected])
                    .setItems(actions) { _, action ->
                        when (action) {
                            0 -> if (selected > 0) {
                                val item = frames.removeAt(selected)
                                frames.add(selected - 1, item)
                            }
                            1 -> if (selected < frames.lastIndex) {
                                val item = frames.removeAt(selected)
                                frames.add(selected + 1, item)
                            }
                            2 -> frames.removeAt(selected)
                        }
                        refreshState(state)
                        if (preview.currentState == state) preview.restart(frames, profile.speedFps(state), state)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
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
        refreshRunningAlives()
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
                refreshRunningAlives()
                Toast.makeText(this, "Alive deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    private fun refreshRunningAlives() {
        if (AliveService.isRunning && Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, AliveService::class.java)
                .setAction(AliveService.ACTION_SHOW_EQUIPPED))
        }
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
