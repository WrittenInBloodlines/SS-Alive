package com.ss.alive.alive

import android.content.Context
import org.json.JSONObject

data class AliveProfile(
    val id: String,
    var name: String,
    var isTemplate: Boolean = false,
    var templateKind: String = "CUSTOM",
    var sizePercent: Int = 55,
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
        const val CLIMB = "CLIMB"
        val STATES = listOf(IDLE, WALK, RUN, SIT, JUMP, FALL, LANDING, HELD, CLIMB)

        fun empty(id: String, name: String): AliveProfile = AliveProfile(id, name)

        fun fromJson(json: String): AliveProfile {
            val root = JSONObject(json)
            val profile = AliveProfile(
                id = root.getString("id"),
                name = root.getString("name"),
                isTemplate = root.optBoolean("template", false),
                templateKind = root.optString("templateKind", "CUSTOM"),
                sizePercent = root.optInt("sizePercent", 55).coerceIn(25, 200)
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
        val root = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("template", isTemplate)
            put("templateKind", templateKind)
            put("sizePercent", sizePercent.coerceIn(25, 200))
        }
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
    private const val KEY_EQUIPPED = "equipped"

    fun save(c: Context, p: AliveProfile) {
        val s = c.getSharedPreferences(PREFS, 0)
        val ids = s.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids += p.id
        s.edit().putStringSet(KEY_IDS, ids).putString("profile_${p.id}", p.toJson()).apply()
    }

    fun get(c: Context, id: String): AliveProfile? = c.getSharedPreferences(PREFS, 0)
        .getString("profile_$id", null)?.let { runCatching { AliveProfile.fromJson(it) }.getOrNull() }

    fun all(c: Context): List<AliveProfile> = c.getSharedPreferences(PREFS, 0)
        .getStringSet(KEY_IDS, emptySet()).orEmpty().mapNotNull { get(c, it) }.sortedBy { it.name.lowercase() }

    fun setActive(c: Context, p: AliveProfile) {
        save(c, p)
        c.getSharedPreferences(PREFS, 0).edit().putString(KEY_ACTIVE, p.id).apply()
    }

    fun active(c: Context): AliveProfile? = c.getSharedPreferences(PREFS, 0)
        .getString(KEY_ACTIVE, null)?.let { get(c, it) }

    fun equippedIds(c: Context): Set<String> = c.getSharedPreferences(PREFS, 0)
        .getStringSet(KEY_EQUIPPED, emptySet()).orEmpty()

    fun equipped(c: Context): List<AliveProfile> = equippedIds(c).mapNotNull { get(c, it) }

    fun isEquipped(c: Context, id: String): Boolean = id in equippedIds(c)

    fun equip(c: Context, p: AliveProfile) {
        save(c, p)
        val ids = equippedIds(c).toMutableSet()
        ids += p.id
        c.getSharedPreferences(PREFS, 0).edit().putStringSet(KEY_EQUIPPED, ids).apply()
    }

    fun unequip(c: Context, id: String) {
        val ids = equippedIds(c).toMutableSet()
        ids.remove(id)
        c.getSharedPreferences(PREFS, 0).edit().putStringSet(KEY_EQUIPPED, ids).apply()
    }

    fun delete(c: Context, id: String) {
        val s = c.getSharedPreferences(PREFS, 0)
        val ids = s.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        ids.remove(id)
        val equipped = equippedIds(c).toMutableSet()
        equipped.remove(id)
        val edit = s.edit().remove("profile_$id").putStringSet(KEY_IDS, ids).putStringSet(KEY_EQUIPPED, equipped)
        if (s.getString(KEY_ACTIVE, null) == id) edit.remove(KEY_ACTIVE)
        edit.apply()
    }

    fun removeLegacyTemplates(c: Context) {
        listOf("template_cat", "template_dog", "template_chick").forEach { delete(c, it) }
    }

    fun template(c: Context, kind: String): AliveProfile {
        val k = kind.uppercase()
        val profile = when (k) {
            "ALEX" -> AliveProfile("template_alex", "Alex", true, "ALEX", 68)
            "CIRO" -> AliveProfile("template_ciro", "Ciro", true, "CIRO", 63)
            else -> throw IllegalArgumentException("Unknown S•S Alive template: $kind")
        }
        save(c, profile)
        return profile
    }

    fun createTemplate(c: Context): AliveProfile = template(c, "ALEX")
}
