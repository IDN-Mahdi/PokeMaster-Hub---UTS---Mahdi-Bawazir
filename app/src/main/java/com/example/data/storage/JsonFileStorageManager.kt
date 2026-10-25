package com.example.data.storage

import android.content.Context
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

class JsonFileStorageManager(private val context: Context) {

    private val autosaveFile: File
        get() = File(context.filesDir, "pokemaster_autosave.json")

    fun saveToFile(
        ratings: Map<Int, PokemonRating>,
        tracker: Map<Int, PokemonTrackerEntry>,
        party: List<PartySlot>,
        mechanicMode: String,
        quizStreak: Int,
        bestStreak: Int
    ): Boolean {
        return try {
            val root = JSONObject()
            root.put("version", 2)
            root.put("timestamp", System.currentTimeMillis())
            root.put("mechanicMode", mechanicMode)
            root.put("quizStreak", quizStreak)
            root.put("bestStreak", bestStreak)

            val ratingsArray = JSONArray()
            ratings.values.forEach { r ->
                if (r.status != RatingStatus.UNRATED || r.tags.isNotEmpty() || r.customTier != null) {
                    val obj = JSONObject()
                    obj.put("pokemonId", r.pokemonId)
                    obj.put("status", r.status.name)
                    val tagArr = JSONArray()
                    r.tags.forEach { tagArr.put(it.name) }
                    obj.put("tags", tagArr)
                    obj.put("customTier", r.customTier?.name)
                    obj.put("updatedAt", r.updatedAt)
                    ratingsArray.put(obj)
                }
            }
            root.put("ratings", ratingsArray)

            val trackerArray = JSONArray()
            tracker.values.forEach { t ->
                if (t.status != TrackerStatus.UNSEEN) {
                    val obj = JSONObject()
                    obj.put("pokemonId", t.pokemonId)
                    obj.put("status", t.status.name)
                    obj.put("updatedAt", t.updatedAt)
                    trackerArray.put(obj)
                }
            }
            root.put("tracker", trackerArray)

            val partyArray = JSONArray()
            party.forEach { p ->
                partyArray.put(serializePartySlot(p))
            }
            root.put("party", partyArray)

            FileWriter(autosaveFile).use { writer ->
                writer.write(root.toString(2))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToJson(
        ratings: Map<Int, PokemonRating>,
        tracker: Map<Int, PokemonTrackerEntry>,
        party: List<PartySlot>,
        mechanicMode: String
    ): String {
        val root = JSONObject()
        root.put("app", "PokéMaster Hub: National 1025 & HGSS Tactical Suite")
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("mechanicMode", mechanicMode)

        val ratingsArray = JSONArray()
        ratings.values.forEach { r ->
            if (r.status != RatingStatus.UNRATED || r.tags.isNotEmpty() || r.customTier != null) {
                val obj = JSONObject()
                obj.put("pokemonId", r.pokemonId)
                obj.put("status", r.status.name)
                val tagArr = JSONArray()
                r.tags.forEach { tagArr.put(it.name) }
                obj.put("tags", tagArr)
                if (r.customTier != null) obj.put("customTier", r.customTier.name)
                ratingsArray.put(obj)
            }
        }
        root.put("ratings", ratingsArray)

        val trackerArray = JSONArray()
        tracker.values.forEach { t ->
            if (t.status != TrackerStatus.UNSEEN) {
                val obj = JSONObject()
                obj.put("pokemonId", t.pokemonId)
                obj.put("status", t.status.name)
                trackerArray.put(obj)
            }
        }
        root.put("tracker", trackerArray)

        val partyArray = JSONArray()
        party.forEach { p ->
            partyArray.put(serializePartySlot(p))
        }
        root.put("party", partyArray)

        return root.toString(2)
    }

    private fun serializePartySlot(p: PartySlot): JSONObject {
        val obj = JSONObject()
        obj.put("slotIndex", p.slotIndex)
        if (p.pokemonId != null) obj.put("pokemonId", p.pokemonId)
        if (p.nickname != null) obj.put("nickname", p.nickname)
        obj.put("move1", p.move1)
        obj.put("move2", p.move2)
        obj.put("move3", p.move3)
        obj.put("move4", p.move4)
        obj.put("item", p.item)
        obj.put("ability", p.ability)
        obj.put("move1Type", p.move1Type.name)
        obj.put("move1Category", p.move1Category)
        obj.put("move2Type", p.move2Type.name)
        obj.put("move2Category", p.move2Category)
        obj.put("move3Type", p.move3Type.name)
        obj.put("move3Category", p.move3Category)
        obj.put("move4Type", p.move4Type.name)
        obj.put("move4Category", p.move4Category)
        if (p.secretType != null) obj.put("secretType", p.secretType.name)
        obj.put("secretCategory", p.secretCategory)
        return obj
    }

    data class ImportResult(
        val ratings: List<PokemonRating>,
        val tracker: List<PokemonTrackerEntry>,
        val party: List<PartySlot>,
        val mechanicMode: String?
    )

    fun importFromJson(jsonString: String): ImportResult? {
        return try {
            val root = JSONObject(jsonString)
            val mechanicMode = root.optString("mechanicMode", null)

            val parsedRatings = mutableListOf<PokemonRating>()
            val ratingsArray = root.optJSONArray("ratings")
            if (ratingsArray != null) {
                for (i in 0 until ratingsArray.length()) {
                    val obj = ratingsArray.getJSONObject(i)
                    val pId = obj.getInt("pokemonId")
                    val status = try { RatingStatus.valueOf(obj.getString("status")) } catch (_: Exception) { RatingStatus.UNRATED }
                    val tags = mutableSetOf<RatingTag>()
                    val tagArr = obj.optJSONArray("tags")
                    if (tagArr != null) {
                        for (j in 0 until tagArr.length()) {
                            try { tags.add(RatingTag.valueOf(tagArr.getString(j))) } catch (_: Exception) {}
                        }
                    }
                    val tier = Tier.fromString(obj.optString("customTier", null))
                    parsedRatings.add(PokemonRating(pId, status, tags, tier))
                }
            }

            val parsedTracker = mutableListOf<PokemonTrackerEntry>()
            val trackerArray = root.optJSONArray("tracker")
            if (trackerArray != null) {
                for (i in 0 until trackerArray.length()) {
                    val obj = trackerArray.getJSONObject(i)
                    val pId = obj.getInt("pokemonId")
                    val status = try { TrackerStatus.valueOf(obj.getString("status")) } catch (_: Exception) { TrackerStatus.UNSEEN }
                    parsedTracker.add(PokemonTrackerEntry(pId, status))
                }
            }

            val parsedParty = mutableListOf<PartySlot>()
            val partyArray = root.optJSONArray("party")
            if (partyArray != null) {
                for (i in 0 until partyArray.length()) {
                    val obj = partyArray.getJSONObject(i)
                    val slotIdx = obj.getInt("slotIndex")
                    val pId = if (obj.has("pokemonId")) obj.getInt("pokemonId") else null
                    val nick = if (obj.has("nickname")) obj.getString("nickname") else null
                    fun parseType(key: String, def: PokemonType): PokemonType {
                        val s = obj.optString(key, def.name)
                        return try { PokemonType.valueOf(s) } catch (_: Exception) { def }
                    }
                    val secStr = obj.optString("secretType", "")
                    val secType = if (secStr.isBlank()) null else try { PokemonType.valueOf(secStr) } catch (_: Exception) { null }
                    parsedParty.add(
                        PartySlot(
                            slotIndex = slotIdx,
                            pokemonId = pId,
                            nickname = nick,
                            move1 = obj.optString("move1", "Tackle"),
                            move2 = obj.optString("move2", "Quick Attack"),
                            move3 = obj.optString("move3", "Protect"),
                            move4 = obj.optString("move4", "Rest"),
                            item = obj.optString("item", "Leftovers"),
                            ability = obj.optString("ability", "Standard"),
                            move1Type = parseType("move1Type", PokemonType.NORMAL),
                            move1Category = obj.optString("move1Category", "Physical"),
                            move2Type = parseType("move2Type", PokemonType.WATER),
                            move2Category = obj.optString("move2Category", "Special"),
                            move3Type = parseType("move3Type", PokemonType.ICE),
                            move3Category = obj.optString("move3Category", "Special"),
                            move4Type = parseType("move4Type", PokemonType.GROUND),
                            move4Category = obj.optString("move4Category", "Physical"),
                            secretType = secType,
                            secretCategory = obj.optString("secretCategory", "Special")
                        )
                    )
                }
            }

            ImportResult(parsedRatings, parsedTracker, parsedParty, mechanicMode)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToMarkdownSummary(
        ratings: Map<Int, PokemonRating>,
        tracker: Map<Int, PokemonTrackerEntry>,
        party: List<PartySlot>,
        allPokemon: List<Pokemon>
    ): String {
        val pokeMap = allPokemon.associateBy { it.id }
        val liked = ratings.values.filter { it.status == RatingStatus.LIKE }
        val dislikes = ratings.values.filter { it.status == RatingStatus.DISLIKE }
        val caughtCount = tracker.values.count { it.status == TrackerStatus.CAUGHT }
        val seenCount = tracker.values.count { it.status == TrackerStatus.SEEN || it.status == TrackerStatus.CAUGHT }

        val sb = StringBuilder()
        sb.append("# PokéMaster Hub Tactical Report\n\n")
        sb.append("Generated on: ${java.util.Date()}\n\n")
        sb.append("## Summary Statistics\n")
        sb.append("- **Total Liked Pokémon:** ${liked.size}\n")
        sb.append("- **Total Disliked Pokémon:** ${dislikes.size}\n")
        sb.append("- **Pokédex Caught:** $caughtCount / 1025 (${String.format("%.1f", caughtCount * 100f / 1025)}%)\n")
        sb.append("- **Pokédex Seen:** $seenCount / 1025\n\n")

        sb.append("## 6-Slot Battle Party Roster\n")
        party.forEachIndexed { i, slot ->
            val mon = slot.pokemonId?.let { pokeMap[it] }
            if (mon != null) {
                sb.append("### Slot ${i + 1}: ${slot.nickname ?: mon.name} (${mon.formattedId()})\n")
                sb.append("- **Type:** ${mon.primaryType.displayName}${mon.secondaryType?.let { " / " + it.displayName } ?: ""}\n")
                sb.append("- **Moves:** 1. ${slot.move1Type.displayName} (${slot.move1Category}) | 2. ${slot.move2Type.displayName} (${slot.move2Category}) | 3. ${slot.move3Type.displayName} (${slot.move3Category}) | 4. ${slot.move4Type.displayName} (${slot.move4Category})\n")
                if (slot.secretType != null) {
                    sb.append("- **Secret Type:** ${slot.secretType.displayName} (${slot.secretCategory})\n")
                }
                sb.append("\n")
            } else {
                sb.append("### Slot ${i + 1}: [Empty Slot]\n\n")
            }
        }

        sb.append("## Top Liked Pokémon by Tier\n")
        val tiers = listOf(Tier.S, Tier.A, Tier.B, Tier.C, Tier.D, Tier.F, Tier.TRAP)
        tiers.forEach { tier ->
            val tierMons = liked.filter { (it.customTier ?: pokeMap[it.pokemonId]?.defaultTier) == tier }
            if (tierMons.isNotEmpty()) {
                sb.append("### Tier ${tier.label} (${tierMons.size})\n")
                tierMons.forEach { r ->
                    val mon = pokeMap[r.pokemonId]
                    if (mon != null) {
                        val tagsStr = if (r.tags.isNotEmpty()) " [${r.tags.joinToString(", ") { it.label }}]" else ""
                        sb.append("- **${mon.name}** ${mon.formattedId()} (BST: ${mon.bst})$tagsStr\n")
                    }
                }
                sb.append("\n")
            }
        }

        return sb.toString()
    }

    fun exportToShowdownFormat(party: List<PartySlot>, allPokemon: List<Pokemon>): String {
        val pokeMap = allPokemon.associateBy { it.id }
        val sb = StringBuilder()

        party.forEach { slot ->
            val mon = slot.pokemonId?.let { pokeMap[it] }
            if (mon != null) {
                val nameDisplay = if (slot.nickname != null && slot.nickname != mon.name) {
                    "${slot.nickname} (${mon.name})"
                } else {
                    mon.name
                }
                sb.append("$nameDisplay @ ${slot.item}\n")
                sb.append("Ability: ${slot.ability}\n")
                if (slot.secretType != null) {
                    sb.append("Tera Type: ${slot.secretType.displayName}\n")
                }
                sb.append("EVs: 252 Atk / 4 SpD / 252 Spe\n")
                sb.append("Jolly Nature\n")
                sb.append("- ${slot.move1Type.displayName} [${slot.move1Category}]\n")
                sb.append("- ${slot.move2Type.displayName} [${slot.move2Category}]\n")
                sb.append("- ${slot.move3Type.displayName} [${slot.move3Category}]\n")
                sb.append("- ${slot.move4Type.displayName} [${slot.move4Category}]\n\n")
            }
        }

        return if (sb.isBlank()) "=== Your Party is Currently Empty ===" else sb.toString().trim()
    }
}
