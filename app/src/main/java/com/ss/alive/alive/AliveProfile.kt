package com.ss.alive.alive

import android.content.Context
import android.net.Uri
import org.json.JSONObject

/** Stores one user-created Alive and the image frames assigned to each state. */
data class AliveProfile(
    val id: String,
    var name: String,
    var isTemplate: Boolean = false,
    val frames: MutableMap<String, MutableList<String>> = mutableMapOf()
) {
    companion object {
        const val IDLE = "IDLE"
        const val WALK = "WALK"
        const val RUN = "RUN"
        const val SIT = "SIT"
        const val JUMP = "JUMP"
        const val FALL = "FALL"
        const val LANDING = "LANDING"
        const val HELD = "HELD"

        val STATES = listOf(IDLE, WALK, RUN, SIT, JUMP, FALL, LANDING, HELD)

        fun empty(id: String, name: String): AliveProfile = AliveProfile(id, name)

        fun fromJson(json: String): AliveProfile {
            val root = JSONObject(json)
            val profile = AliveProfile(
                id = root.getString("id"),
                name = root.getString("name"),
                isTemplate = root.optBoolean("template", false)
            )
            val frameObject = root.optJSONObject("frames") ?: return profile
            STATES.forEach { state ->
                val array = frameObject.optJSONArray(state) ?: return@forEach
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) list += array.getString(i)
                profile.frames[state] = list
            }
            return profile
        }
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        root.put("template", isTemplate)
        val frameObject = JSONObject()
        frames.forEach { (state, uris) ->
            val array = org.json.JSONArray()
            uris.forEach(array::put)
            frameObject.put(state, array)
        }
        root.put("frames", frameObject)
        return root.toString()
    }

    fun frameUris(state: String): List<String> = frames[state].orEmpty()
}

object AliveRepository {
    private const val PREFS = "ss_alive_profiles"
    private const val KEY_IDS = "ids"
    private const val KEY_ACTIVE = "active"

    fun save(context: Context, profile: AliveProfile) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids += profile.id
        prefs.edit()
            .putStringSet(KEY_IDS, ids)
            .putString("profile_${profile.id}", profile.toJson())
            .apply()
    }

    fun get(context: Context, id: String): AliveProfile? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("profile_$id", null) ?: return null
        return runCatching { AliveProfile.fromJson(json) }.getOrNull()
    }

    fun all(context: Context): List<AliveProfile> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_IDS, emptySet()).orEmpty()
            .mapNotNull { get(context, it) }
            .sortedBy { it.name.lowercase() }
    }

    fun setActive(context: Context, profile: AliveProfile) {
        save(context, profile)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE, profile.id).apply()
    }

    fun active(context: Context): AliveProfile? {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE, null) ?: return null
        return get(context, id)
    }

    fun createTemplate(context: Context): AliveProfile {
        val profile = AliveProfile("template_cat", "Cat", isTemplate = true)
        save(context, profile)
        setActive(context, profile)
        return profile
    }
}
