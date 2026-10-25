package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PokemonType
import org.json.JSONArray
import org.json.JSONObject

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pokemaster_user_prefs", Context.MODE_PRIVATE)

    var mechanicMode: String
        get() = prefs.getString("key_mechanic_mode", "modern") ?: "modern"
        set(value) = prefs.edit().putString("key_mechanic_mode", value).apply()

    val isModern: Boolean
        get() = mechanicMode == "modern"

    var isShinyGlobal: Boolean
        get() = prefs.getBoolean("key_shiny_global", false)
        set(value) = prefs.edit().putBoolean("key_shiny_global", value).apply()

    var quizStreak: Int
        get() = prefs.getInt("key_quiz_streak", 0)
        set(value) = prefs.edit().putInt("key_quiz_streak", value).apply()

    var bestStreak: Int
        get() = prefs.getInt("key_best_streak", 0)
        set(value) = prefs.edit().putInt("key_best_streak", value).apply()

    var lastSelectedFilter: String
        get() = prefs.getString("key_last_filter", "ALL") ?: "ALL"
        set(value) = prefs.edit().putString("key_last_filter", value).apply()

    // Johto Mode A Quota per type (default 3 per type, customizable 1..10)
    fun getTypeQuota(type: PokemonType): Int {
        return prefs.getInt("key_quota_${type.name}", 3)
    }

    fun setTypeQuota(type: PokemonType, quota: Int) {
        prefs.edit().putInt("key_quota_${type.name}", quota.coerceIn(1, 10)).apply()
    }

    fun setBatchTypeQuota(quota: Int) {
        val editor = prefs.edit()
        PokemonType.entries.forEach { type ->
            editor.putInt("key_quota_${type.name}", quota.coerceIn(1, 10))
        }
        editor.apply()
    }

    // Preferred stage per family (e.g. family "fam_133" -> Umbreon #197)
    fun getPreferredStage(familyId: String): Int? {
        val id = prefs.getInt("key_pref_stage_$familyId", -1)
        return if (id > 0) id else null
    }

    fun setPreferredStage(familyId: String, pokemonId: Int) {
        prefs.edit().putInt("key_pref_stage_$familyId", pokemonId).apply()
    }

    // Johto Mode B: Free Ranker Top 50 list of IDs
    fun getFreeRankerTop50(): List<Int> {
        val json = prefs.getString("key_free_ranker_top50", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Int>()
            for (i in 0 until array.length()) {
                list.add(array.getInt(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setFreeRankerTop50(ids: List<Int>) {
        val array = JSONArray()
        ids.take(50).forEach { array.put(it) }
        prefs.edit().putString("key_free_ranker_top50", array.toString()).apply()
    }

    // Type Specialists Assignments (Mode A)
    fun getTypeAssignments(type: PokemonType): List<Int> {
        val json = prefs.getString("key_type_assign_${type.name}", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Int>()
            for (i in 0 until array.length()) {
                list.add(array.getInt(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setTypeAssignments(type: PokemonType, ids: List<Int>) {
        val array = JSONArray()
        val quota = getTypeQuota(type)
        ids.take(quota).forEach { array.put(it) }
        prefs.edit().putString("key_type_assign_${type.name}", array.toString()).apply()
    }

    // Preferred Shiny Pokémon IDs (per-Pokémon appearance toggle)
    fun getPreferredShinyIds(): Set<Int> {
        val stringSet = prefs.getStringSet("key_preferred_shiny_ids", emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun setPreferredShinyIds(ids: Set<Int>) {
        val stringSet = ids.map { it.toString() }.toSet()
        prefs.edit().putStringSet("key_preferred_shiny_ids", stringSet).apply()
    }
}
