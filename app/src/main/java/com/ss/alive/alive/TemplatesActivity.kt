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
import java.io.ByteArrayOutputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class TemplatesActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private val pickTemplateImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) createTemplateWithImage(uri)
    }

    private val templateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AccountBridge.TEMPLATE_RESULT_ACTION) return
            val error = intent.getStringExtra(AccountBridge.EXTRA_TEMPLATE_ERROR)
            if (error != null) {
                Toast.makeText(this@TemplatesActivity, "Template error: $error", Toast.LENGTH_SHORT).show()
                return
            }
            renderTemplates(intent.getStringExtra(AccountBridge.EXTRA_TEMPLATE_JSON).orEmpty())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 48)
        }
        setContentView(root)
        registerTemplateReceiver()
        showLoading()
        AccountBridge.requestTemplates(this)
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

    private fun showLoading() {
        root.removeAllViews()
        root.addView(TextView(this).apply { text = "TEMPLATES"; textSize = 30f })
        root.addView(TextView(this).apply {
            text = "Loading S•S templates..."
            textSize = 16f
            setPadding(0, 18, 0, 18)
        })
    }

    private fun renderTemplates(json: String) {
        root.removeAllViews()
        root.addView(TextView(this).apply { text = "TEMPLATES"; textSize = 30f })

        if (AliveAccountStore.get(this)?.isCreator == true) {
            root.addView(Button(this).apply {
                text = "+  CREATE TEMPLATE"
                setOnClickListener { showCreateTemplateDialog() }
            })
        }

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val array = runCatching { JSONArray(json) }.getOrElse { JSONArray() }
        if (array.length() == 0) {
            list.addView(TextView(this).apply {
                text = "No templates published yet."
                textSize = 16f
                setPadding(0, 24, 0, 24)
            })
        }

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 20, 0, 20)
            }
            val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            runCatching {
                val bytes = Base64.decode(item.optString("imageData"), Base64.DEFAULT)
                image.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            }
            card.addView(image, LinearLayout.LayoutParams(180, 180))

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 0, 0, 0)
            }
            info.addView(TextView(this).apply { text = item.optString("name", "Unnamed Template"); textSize = 20f })
            info.addView(TextView(this).apply {
                text = item.optString("description", "")
                textSize = 14f
                setPadding(0, 6, 0, 8)
            })
            info.addView(TextView(this).apply { text = "by ${item.optString("author", "S•S")}"; textSize = 12f })
            info.addView(Button(this).apply {
                text = "USE TEMPLATE"
                setOnClickListener { installTemplate(item) }
            })
            card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
            list.addView(card)
        }

        root.addView(Button(this).apply { text = "BACK"; setOnClickListener { finish() } })
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
                if (n.isEmpty()) {
                    Toast.makeText(this, "Enter a template name first.", Toast.LENGTH_SHORT).show()
                } else {
                    getPreferences(MODE_PRIVATE).edit()
                        .putString("pending_template_name", n)
                        .putString("pending_template_description", d)
                        .apply()
                    pickTemplateImage.launch("image/*")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createTemplateWithImage(uri: android.net.Uri) {
        val prefs = getPreferences(MODE_PRIVATE)
        val name = prefs.getString("pending_template_name", "") ?: ""
        val description = prefs.getString("pending_template_description", "") ?: ""
        val bitmap = runCatching { contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } }.getOrNull()
        if (bitmap == null) {
            Toast.makeText(this, "Could not read that image.", Toast.LENGTH_SHORT).show()
            return
        }
        AccountBridge.createTemplate(this, name, description, compressForTemplate(bitmap))
        Toast.makeText(this, "Publishing template...", Toast.LENGTH_SHORT).show()
    }

    private fun compressForTemplate(source: Bitmap): String {
        val max = 256
        val scale = minOf(1f, max.toFloat() / maxOf(source.width, source.height))
        val scaled = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun installTemplate(item: JSONObject) {
        val imageData = item.optString("imageData")
        if (imageData.isEmpty()) return
        runCatching {
            val bytes = Base64.decode(imageData, Base64.DEFAULT)
            val file = java.io.File(filesDir, "template_${UUID.randomUUID()}.jpg")
            file.writeBytes(bytes)
            val profile = AliveProfile.empty("alive_${System.currentTimeMillis()}", item.optString("name", "Template"))
            profile.frames.getOrPut(AliveProfile.IDLE) { mutableListOf() }.add("file://${file.absolutePath}")
            AliveRepository.save(this, profile)
            Toast.makeText(this, "Template added to your Alives.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Could not install template.", Toast.LENGTH_SHORT).show()
        }
    }
}
