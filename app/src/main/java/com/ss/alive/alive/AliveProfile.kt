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
                id = root.getString("id"), name = root.getString("name"),
                isTemplate = root.optBoolean("template", false),
                templateKind = root.optString("templateKind", if (root.optBoolean("template", false)) "CAT" else "CUSTOM"),
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
            put("id", id); put("name", name); put("template", isTemplate); put("templateKind", templateKind)
            put("sizePercent", sizePercent.coerceIn(25, 200))
        }
        val frameObject = JSONObject()
        frames.forEach { (state, uris) ->
            val array = org.json.JSONArray(); uris.forEach(array::put); frameObject.put(state, array)
        }
        root.put("frames", frameObject)
        return root.toString()
    }

    fun frameUris(state: String): List<String> = frames[state].orEmpty()
}

object AliveRepository {
private const val PREFS="ss_alive_profiles";private const val KEY_IDS="ids";private const val KEY_ACTIVE="active";private const val KEY_EQUIPPED="equipped"
fun save(c:Context,p:AliveProfile){val s=c.getSharedPreferences(PREFS,0);val ids=s.getStringSet(KEY_IDS,emptySet()).orEmpty().toMutableSet();ids+=p.id;s.edit().putStringSet(KEY_IDS,ids).putString("profile_${p.id}",p.toJson()).apply()}
fun get(c:Context,id:String)=c.getSharedPreferences(PREFS,0).getString("profile_$id",null)?.let{runCatching{AliveProfile.fromJson(it)}.getOrNull()}
fun all(c:Context)=c.getSharedPreferences(PREFS,0).getStringSet(KEY_IDS,emptySet()).orEmpty().mapNotNull{get(c,it)}.sortedBy{it.name.lowercase()}
fun setActive(c:Context,p:AliveProfile){save(c,p);c.getSharedPreferences(PREFS,0).edit().putString(KEY_ACTIVE,p.id).apply()}
fun active(c:Context)=c.getSharedPreferences(PREFS,0).getString(KEY_ACTIVE,null)?.let{get(c,it)}
fun equippedIds(c:Context)=c.getSharedPreferences(PREFS,0).getStringSet(KEY_EQUIPPED,emptySet()).orEmpty()
fun equipped(c:Context)=equippedIds(c).mapNotNull{get(c,it)}
fun isEquipped(c:Context,id:String)=id in equippedIds(c)
fun equip(c:Context,p:AliveProfile){save(c,p);val x=equippedIds(c).toMutableSet();x+=p.id;c.getSharedPreferences(PREFS,0).edit().putStringSet(KEY_EQUIPPED,x).apply()}
fun unequip(c:Context,id:String){val x=equippedIds(c).toMutableSet();x.remove(id);c.getSharedPreferences(PREFS,0).edit().putStringSet(KEY_EQUIPPED,x).apply()}
fun delete(c:Context,id:String){val s=c.getSharedPreferences(PREFS,0);val ids=s.getStringSet(KEY_IDS,emptySet()).orEmpty().toMutableSet();ids.remove(id);val e=equippedIds(c).toMutableSet();e.remove(id);val ed=s.edit().remove("profile_$id").putStringSet(KEY_IDS,ids).putStringSet(KEY_EQUIPPED,e);if(s.getString(KEY_ACTIVE,null)==id)ed.remove(KEY_ACTIVE);ed.apply()}
fun template(c:Context,kind:String):AliveProfile{val k=kind.uppercase();val n=when(k){"DOG"->"Dog";"CHICK"->"Chick";else->"Cat"};val p=AliveProfile("template_"+k.lowercase(),n,true,k,if(k=="DOG")65 else if(k=="CHICK")42 else 55);save(c,p);return p}
fun createTemplate(c:Context)=template(c,"CAT")
}