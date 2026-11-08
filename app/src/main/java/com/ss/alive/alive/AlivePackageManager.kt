package com.ss.alive.alive

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Portable .ssalive packages contain the profile JSON and copies of its frame/sound files.
 * Import paths are validated and extracted into this app's private files directory.
 */
object AlivePackageManager {
    private const val MAX_PACKAGE_BYTES = 30L * 1024L * 1024L

    fun export(context: Context, profile: AliveProfile, destination: Uri) {
        val root = JSONObject(profile.toJson())
        root.put("packageVersion", 1)
        val frameObject = root.optJSONObject("frames") ?: JSONObject()
        val assets = linkedMapOf<String, ByteArray>()

        AliveProfile.STATES.forEach { state ->
            val original = frameObject.optJSONArray(state) ?: return@forEach
            val portable = org.json.JSONArray()
            for (index in 0 until original.length()) {
                val source = original.optString(index)
                val entryName = "frames/${state.lowercase()}_${index}.png"
                copyUriIntoPackage(context, source, entryName, assets)
                portable.put(entryName)
            }
            frameObject.put(state, portable)
        }
        root.put("frames", frameObject)

        val sound = root.optString("soundUri", "")
        if (sound.isNotBlank()) {
            val entryName = "sounds/effect"
            copyUriIntoPackage(context, sound, entryName, assets)
            root.put("soundUri", entryName)
        }
        if (assets.values.sumOf { it.size.toLong() } > MAX_PACKAGE_BYTES) {
            throw IllegalArgumentException("The complete Alive package is larger than the 30 MB limit.")
        }

        context.contentResolver.openOutputStream(destination)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("profile.json"))
                zip.write(root.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                assets.forEach { (entryName, sourceUri) ->
                    zip.putNextEntry(ZipEntry(entryName))
                    zip.write(sourceUri)
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Could not create the selected package file.")
    }

    private fun copyUriIntoPackage(
        context: Context,
        source: String,
        entryName: String,
        assets: MutableMap<String, ByteArray>
    ) {
        if (source.isBlank()) throw IllegalArgumentException("An animation asset has an empty location.")
        val bytes = context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
            ?: throw IllegalStateException("An animation or sound file is no longer accessible.")
        if (assets.values.sumOf { it.size.toLong() } + bytes.size > MAX_PACKAGE_BYTES) {
            throw IllegalArgumentException("The complete Alive package is larger than the 30 MB limit.")
        }
        assets[entryName] = bytes
    }

    fun import(context: Context, source: Uri): AliveProfile {
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }
                    val name = entry.name
                    if (name.startsWith("/") || name.contains("..") || name !in listOf("profile.json") &&
                        !name.startsWith("frames/") && !name.startsWith("sounds/")) {
                        throw IllegalArgumentException("The selected file contains an unsupported path.")
                    }
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_PACKAGE_BYTES) {
                            throw IllegalArgumentException("The package is larger than the 30 MB limit.")
                        }
                        output.write(buffer, 0, count)
                    }
                    entries[name] = output.toByteArray()
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Could not open the selected package.")

        val jsonBytes = entries["profile.json"]
            ?: throw IllegalArgumentException("This file is missing its Alive profile.")
        val root = JSONObject(String(jsonBytes, Charsets.UTF_8))
        if (root.optInt("packageVersion", -1) != 1) {
            throw IllegalArgumentException("This Alive package version is not supported.")
        }
        val newId = "alive_${UUID.randomUUID()}"
        root.put("id", newId)
        root.put("template", false)
        val base = File(context.filesDir, "alive_imports/$newId").canonicalFile
        if (!base.exists() && !base.mkdirs()) throw IllegalStateException("Could not prepare the import folder.")

        fun restoreAsset(entryName: String): String {
            val bytes = entries[entryName] ?: throw IllegalArgumentException("The package is missing $entryName.")
            val target = File(base, entryName).canonicalFile
            if (!target.path.startsWith(base.path + File.separator)) {
                throw IllegalArgumentException("The package contains an invalid file path.")
            }
            target.parentFile?.mkdirs()
            target.writeBytes(bytes)
            return Uri.fromFile(target).toString()
        }

        val frameObject = root.optJSONObject("frames") ?: JSONObject()
        AliveProfile.STATES.forEach { state ->
            val portable = frameObject.optJSONArray(state) ?: return@forEach
            val restored = org.json.JSONArray()
            for (index in 0 until portable.length()) {
                restored.put(restoreAsset(portable.getString(index)))
            }
            frameObject.put(state, restored)
        }
        root.put("frames", frameObject)

        val sound = root.optString("soundUri", "")
        if (sound.startsWith("sounds/")) root.put("soundUri", restoreAsset(sound))

        return AliveProfile.fromJson(root.toString())
    }
}
