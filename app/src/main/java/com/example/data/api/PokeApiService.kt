package com.example.data.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class PokemonLiveDetail(
    val id: Int,
    val name: String,
    val heightMeters: Float,
    val weightKg: Float,
    val baseExperience: Int,
    val abilities: List<String>,
    val movesSummary: List<String>,
    val flavorText: String,
    val habitat: String?,
    val captureRate: Int?,
    val isLiveSynced: Boolean = true
)

class PokeApiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // In-memory cache
    private val memoryCache = mutableMapOf<Int, PokemonLiveDetail>()

    // Disk cache directory
    private val cacheDir = File(context.cacheDir, "pokeapi_cache").apply { mkdirs() }

    suspend fun fetchPokemonDetails(pokemonId: Int): PokemonLiveDetail = withContext(Dispatchers.IO) {
        // Check memory cache
        memoryCache[pokemonId]?.let { return@withContext it }

        // Check disk cache
        val diskFile = File(cacheDir, "pokemon_$pokemonId.json")
        if (diskFile.exists()) {
            try {
                val cachedJson = JSONObject(diskFile.readText())
                val cached = parseCachedDetail(cachedJson)
                memoryCache[pokemonId] = cached
                return@withContext cached
            } catch (_: Exception) {}
        }

        // Live HTTP GET request to PokeAPI v2
        try {
            val pokemonUrl = "https://pokeapi.co/api/v2/pokemon/$pokemonId"
            val speciesUrl = "https://pokeapi.co/api/v2/pokemon-species/$pokemonId"

            val req1 = Request.Builder().url(pokemonUrl).header("User-Agent", "PokeMasterHub/1.0").build()
            val res1 = client.newCall(req1).execute()
            val json1 = JSONObject(res1.body?.string() ?: "{}")

            // Parse Pokemon core data
            val name = json1.optString("name", "Pokemon #$pokemonId")
            val heightDm = json1.optInt("height", 10)
            val weightHg = json1.optInt("weight", 100)
            val baseExp = json1.optInt("base_experience", 100)

            val abilitiesList = mutableListOf<String>()
            val abilitiesArr = json1.optJSONArray("abilities")
            if (abilitiesArr != null) {
                for (i in 0 until abilitiesArr.length()) {
                    val abObj = abilitiesArr.getJSONObject(i)
                    val abName = abObj.getJSONObject("ability").optString("name", "")
                    if (abName.isNotBlank()) abilitiesList.add(abName.replace("-", " ").replaceFirstChar { it.uppercase() })
                }
            }

            val movesList = mutableListOf<String>()
            val movesArr = json1.optJSONArray("moves")
            if (movesArr != null) {
                for (i in 0 until minOf(movesArr.length(), 20)) {
                    val mvObj = movesArr.getJSONObject(i)
                    val mvName = mvObj.getJSONObject("move").optString("name", "")
                    if (mvName.isNotBlank()) movesList.add(mvName.replace("-", " ").replaceFirstChar { it.uppercase() })
                }
            }

            // Request 2: Species data for Flavor Text & Habitat
            var flavorText = "A mysterious Pokémon discovered in the vast wilderness of the Pokémon world."
            var habitat: String? = null
            var captureRate: Int? = null

            try {
                val req2 = Request.Builder().url(speciesUrl).header("User-Agent", "PokeMasterHub/1.0").build()
                val res2 = client.newCall(req2).execute()
                val json2 = JSONObject(res2.body?.string() ?: "{}")

                captureRate = json2.optInt("capture_rate", 45)
                val habitatObj = json2.optJSONObject("habitat")
                habitat = habitatObj?.optString("name")?.replaceFirstChar { it.uppercase() }

                val flavorEntries = json2.optJSONArray("flavor_text_entries")
                if (flavorEntries != null) {
                    for (i in 0 until flavorEntries.length()) {
                        val entry = flavorEntries.getJSONObject(i)
                        val lang = entry.getJSONObject("language").optString("name")
                        if (lang == "en") {
                            flavorText = entry.optString("flavor_text", "").replace("\n", " ").replace("\u000c", " ")
                            break
                        }
                    }
                }
            } catch (_: Exception) {}

            val detail = PokemonLiveDetail(
                id = pokemonId,
                name = name.replaceFirstChar { it.uppercase() },
                heightMeters = heightDm / 10.0f,
                weightKg = weightHg / 10.0f,
                baseExperience = baseExp,
                abilities = if (abilitiesList.isNotEmpty()) abilitiesList else listOf("Standard Ability"),
                movesSummary = if (movesList.isNotEmpty()) movesList else listOf("Tackle", "Quick Attack"),
                flavorText = flavorText,
                habitat = habitat,
                captureRate = captureRate,
                isLiveSynced = true
            )

            // Save to memory and disk cache
            memoryCache[pokemonId] = detail
            saveToDiskCache(diskFile, detail)

            return@withContext detail
        } catch (e: Exception) {
            e.printStackTrace()
            // Offline Fallback Data
            val fallback = PokemonLiveDetail(
                id = pokemonId,
                name = "Mon #$pokemonId",
                heightMeters = 1.0f,
                weightKg = 30.0f,
                baseExperience = 120,
                abilities = listOf("Inner Focus", "Overgrow"),
                movesSummary = listOf("Tackle", "Growl", "Quick Attack", "Protect"),
                flavorText = "Offline cached profile: An energetic Pokémon revered across multiple battle frontiers.",
                habitat = "Grassland",
                captureRate = 45,
                isLiveSynced = false
            )
            memoryCache[pokemonId] = fallback
            return@withContext fallback
        }
    }

    private fun saveToDiskCache(file: File, detail: PokemonLiveDetail) {
        try {
            val obj = JSONObject().apply {
                put("id", detail.id)
                put("name", detail.name)
                put("heightMeters", detail.heightMeters.toDouble())
                put("weightKg", detail.weightKg.toDouble())
                put("baseExperience", detail.baseExperience)
                put("abilities", org.json.JSONArray(detail.abilities))
                put("movesSummary", org.json.JSONArray(detail.movesSummary))
                put("flavorText", detail.flavorText)
                if (detail.habitat != null) put("habitat", detail.habitat)
                if (detail.captureRate != null) put("captureRate", detail.captureRate)
            }
            file.writeText(obj.toString())
        } catch (_: Exception) {}
    }

    private fun parseCachedDetail(json: JSONObject): PokemonLiveDetail {
        val abList = mutableListOf<String>()
        val abArr = json.optJSONArray("abilities")
        if (abArr != null) {
            for (i in 0 until abArr.length()) abList.add(abArr.getString(i))
        }

        val mvList = mutableListOf<String>()
        val mvArr = json.optJSONArray("movesSummary")
        if (mvArr != null) {
            for (i in 0 until mvArr.length()) mvList.add(mvArr.getString(i))
        }

        return PokemonLiveDetail(
            id = json.getInt("id"),
            name = json.getString("name"),
            heightMeters = json.getDouble("heightMeters").toFloat(),
            weightKg = json.getDouble("weightKg").toFloat(),
            baseExperience = json.getInt("baseExperience"),
            abilities = abList,
            movesSummary = mvList,
            flavorText = json.getString("flavorText"),
            habitat = json.optString("habitat", null),
            captureRate = if (json.has("captureRate")) json.getInt("captureRate") else null,
            isLiveSynced = true
        )
    }
}
