package com.ss.alive.alive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ss.alive.account.AccountBridge
import com.ss.alive.account.AliveAccountStore
import com.ss.alive.account.AccountBridgeReceiver
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import com.ss.alive.ui.AliveUi

class TemplatesActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout

    private val pickTemplateImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) createTemplateWithImage(uri)
    }

    private val templateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AccountBridge.TEMPLATE_RESULT_ACTION) return
            consumeTemplateResult(
                intent.getStringExtra(AccountBridge.EXTRA_TEMPLATE_JSON),
                intent.getStringExtra(AccountBridge.EXTRA_TEMPLATE_ERROR)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (scroll, content) = AliveUi.scrollPage(this)
        root = content
        setContentView(scroll)
        registerTemplateReceiver()
        clearStoredTemplateResult()
        showLoading()
        if (!AccountBridge.requestTemplates(this)) {
            showBridgeError("HUB_NOT_INSTALLED")
        }
    }

    override fun onResume() {
        super.onResume()
        consumeStoredTemplateResult()
    }

    override fun onDestroy() {
        unregisterReceiver(templateReceiver)
        super.onDestroy()
    }

    private fun registerTemplateReceiver() {
        val filter = IntentFilter(AccountBridge.TEMPLATE_RESULT_ACTION)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(templateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(templateReceiver, filter)
        }
    }

    private fun consumeStoredTemplateResult() {
        val prefs = getSharedPreferences(AccountBridgeReceiver.TEMPLATE_RESULT_PREFS, MODE_PRIVATE)
        val json = prefs.getString(AccountBridgeReceiver.TEMPLATE_RESULT_JSON, null)
        val error = prefs.getString(AccountBridgeReceiver.TEMPLATE_RESULT_ERROR, null)
        if (json == null && error == null) return

        clearStoredTemplateResult()
        consumeTemplateResult(json, error)
    }

    private fun clearStoredTemplateResult() {
        getSharedPreferences(AccountBridgeReceiver.TEMPLATE_RESULT_PREFS, MODE_PRIVATE)
            .edit()
            .remove(AccountBridgeReceiver.TEMPLATE_RESULT_JSON)
            .remove(AccountBridgeReceiver.TEMPLATE_RESULT_ERROR)
            .apply()
    }

    private fun consumeTemplateResult(json: String?, error: String?) {
        if (error != null) {
            showBridgeError(error)
            return
        }
        renderTemplates(json.orEmpty())
    }

    private fun showBridgeError(error: String) {
        root.removeAllViews()
        root.addView(AliveUi.label(this, "S•S ALIVE  /  LIBRARY"))
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.heading(this, "Templates unavailable", 25f))
        val message = when (error) {
            "NOT_SIGNED_IN" -> "Open S•S Hub and sign in with your S•S account, then return and try again."
            "EMAIL_NOT_VERIFIED" -> "Verify your email address in S•S Hub before using connected features."
            "HUB_NOT_INSTALLED" -> "S•S Hub must be installed to load shared templates."
            "PROFILE_INVALID" -> "Your Hub profile could not be verified. Open S•S Hub and sign in again."
            else -> "S•S Hub could not provide templates ($error). Check your connection and try again."
        }
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.body(this, message, 14f))
        root.addView(AliveUi.gap(this, 16))
        root.addView(AliveUi.button(this, "Open S•S Hub", true) {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.ss.hub")
            if (launchIntent == null) {
                Toast.makeText(this, "S•S Hub is not installed.", Toast.LENGTH_LONG).show()
            } else {
                startActivity(launchIntent)
            }
        })
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.button(this, "Try again", false) {
            showLoading()
            if (!AccountBridge.requestTemplates(this)) showBridgeError("HUB_NOT_INSTALLED")
        })
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.button(this, "Back", false) { finish() })
    }

    private fun showLoading() {
        root.removeAllViews()
        root.addView(AliveUi.label(this, "S•S ALIVE  /  LIBRARY"))
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.heading(this, "Templates", 28f))
        root.addView(AliveUi.body(this, "Discover ready-made characters for your screen."))
        root.addView(AliveUi.gap(this, 20))
        val loading = AliveUi.column(this)
        loading.addView(AliveUi.heading(this, "Loading templates…", 18f))
        loading.addView(AliveUi.body(this, "This may take a moment."))
        root.addView(AliveUi.card(this, loading))
    }

    private fun renderTemplates(json: String) {
        root.removeAllViews()
        root.addView(AliveUi.label(this, "S•S ALIVE  /  LIBRARY"))
        root.addView(AliveUi.gap(this, 8))
        root.addView(AliveUi.heading(this, "Templates", 28f))
        root.addView(AliveUi.body(this, "Find a character you like and add it to your Alives."))
        root.addView(AliveUi.gap(this, 16))

        if (AliveAccountStore.get(this)?.isCreator == true) {
            root.addView(AliveUi.button(this, "Create a template", true) {
                showCreateTemplateDialog()
            })
            root.addView(AliveUi.gap(this, 14))
        }

        val array = runCatching { JSONArray(json) }.getOrElse { JSONArray() }
        if (array.length() == 0) {
            val empty = AliveUi.column(this)
            empty.addView(AliveUi.heading(this, "Nothing here yet", 18f))
            empty.addView(AliveUi.body(this, "No templates have been published yet. Check back later."))
            root.addView(AliveUi.card(this, empty))
        }

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val card = AliveUi.column(this)
            val top = AliveUi.row(this)
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(AliveUi.surfaceRaised)
            }
            runCatching {
                val bytes = Base64.decode(item.optString("imageData"), Base64.DEFAULT)
                image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }
            top.addView(image, LinearLayout.LayoutParams(
                AliveUi.dp(this, 92), AliveUi.dp(this, 92)
            ))
            val info = AliveUi.column(this)
            info.setPadding(AliveUi.dp(this, 12), 0, 0, 0)
            info.addView(AliveUi.heading(this, item.optString("name", "Unnamed template"), 17f))
            info.addView(AliveUi.body(this, "by ${item.optString("author", "S•S")}", 12f))
            top.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(top)
            val description = item.optString("description", "").trim()
            if (description.isNotEmpty()) {
                card.addView(AliveUi.gap(this, 10))
                card.addView(AliveUi.body(this, description, 13f))
            }
            card.addView(AliveUi.gap(this, 12))
            card.addView(AliveUi.button(this, "Add to my Alives", true) {
                installTemplate(item)
            })
            root.addView(AliveUi.card(this, card))
        }

        root.addView(AliveUi.gap(this, 4))
        root.addView(AliveUi.button(this, "Back", false) { finish() })
    }

    private fun showCreateTemplateDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }
        val name = EditText(this).apply { hint = "Template name"; setSingleLine(true) }
        val description = EditText(this).apply { hint = "Description"; minLines = 3 }
        box.addView(name)
        box.addView(description)

        AlertDialog.Builder(this)
            .setTitle("Create S•S Template")
            .setView(box)
            .setPositiveButton("Choose photo") { _, _ ->
                val n = name.text.toString().trim()
                val d = description.text.toString().trim()
                when {
                    n.isEmpty() -> {
                        Toast.makeText(this, "Enter a template name first.", Toast.LENGTH_SHORT).show()
                    }
                    n.length > 80 -> {
                        Toast.makeText(this, "Template names must be 80 characters or fewer.", Toast.LENGTH_LONG).show()
                    }
                    d.length > 2000 -> {
                        Toast.makeText(this, "Descriptions must be 2000 characters or fewer.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        getPreferences(MODE_PRIVATE).edit()
                            .putString("pending_template_name", n)
                            .putString("pending_template_description", d)
                            .apply()
                        pickTemplateImage.launch("image/*")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createTemplateWithImage(uri: android.net.Uri) {
        val prefs = getPreferences(MODE_PRIVATE)
        val name = prefs.getString("pending_template_name", "") ?: ""
        val description = prefs.getString("pending_template_description", "") ?: ""
        val bitmap = runCatching {
            contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        if (bitmap == null) {
            Toast.makeText(this, "Could not read that image.", Toast.LENGTH_SHORT).show()
            return
        }
        val started = AccountBridge.createTemplate(this, name, description, compressForTemplate(bitmap))
        if (started) {
            Toast.makeText(this, "Publishing template...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Could not open S•S Hub. Make sure it is installed.", Toast.LENGTH_LONG).show()
        }
    }

    private fun compressForTemplate(source: Bitmap): String {
        val max = 128
        val scale = minOf(1f, max.toFloat() / maxOf(source.width, source.height))
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 55, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun installTemplate(item: JSONObject) {
        val imageData = item.optString("imageData")
        if (imageData.isEmpty()) return
        runCatching {
            val bytes = Base64.decode(imageData, Base64.DEFAULT)
            val file = java.io.File(filesDir, "template_${UUID.randomUUID()}.jpg")
            file.writeBytes(bytes)
            val profile = AliveProfile.empty(
                "alive_${System.currentTimeMillis()}",
                item.optString("name", "Template")
            )
            profile.frames.getOrPut(AliveProfile.IDLE) { mutableListOf() }
                .add("file://${file.absolutePath}")
            AliveRepository.save(this, profile)
            Toast.makeText(this, "Template added to your Alives.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Could not install template.", Toast.LENGTH_SHORT).show()
        }
    }
}
