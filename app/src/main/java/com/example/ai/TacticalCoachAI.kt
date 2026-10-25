package com.example.ai

import com.example.data.model.PartySlot
import com.example.data.model.Pokemon
import com.example.data.model.PokemonType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TacticalCoachAnalysis(
    val title: String,
    val overallRating: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>,
    val summary: String
)

data class AiChatMessage(
    val id: Long = System.nanoTime(),
    val isUser: Boolean,
    val senderLabel: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PartyMoveBreakdown(
    val coveredTypes: Set<PokemonType>,
    val blindSpots: List<PokemonType>,
    val physicalCount: Int,
    val specialCount: Int,
    val typeHitCounts: Map<PokemonType, Int>,       // Number of team members (0..6) covering each type
    val typeTotalMoveHits: Map<PokemonType, Int> = emptyMap() // Total individual moves hitting each type
)

object TacticalCoachAI {

    fun extractSlotMoveTypesAndCategories(
        slot: PartySlot,
        mon: Pokemon?,
        isModern: Boolean
    ): List<Pair<PokemonType, String>> {
        val activeTypes = PokemonType.activeTypes(isModern).toSet()
        val rawPairs = mutableListOf(
            Pair(slot.move1Type, slot.move1Category),
            Pair(slot.move2Type, slot.move2Category),
            Pair(slot.move3Type, slot.move3Category),
            Pair(slot.move4Type, slot.move4Category)
        )
        slot.secretType?.let { secT ->
            rawPairs.add(Pair(secT, slot.secretCategory))
        }
        return rawPairs.mapNotNull { (pType, catStr) ->
            if (activeTypes.contains(pType)) {
                val cleanCat = if (catStr.equals("Special", ignoreCase = true)) "Special" else "Physical"
                Pair(pType, cleanCat)
            } else null
        }
    }

    fun computeMoveCoverageBreakdown(
        party: List<PartySlot>,
        pokeMap: Map<Int, Pokemon>,
        isModern: Boolean
    ): PartyMoveBreakdown {
        val allTypes = PokemonType.activeTypes(isModern)
        val activeSlots = party.filter { it.pokemonId != null }

        var physCount = 0
        var specCount = 0
        val perSlotMoves = mutableListOf<List<PokemonType>>()

        activeSlots.forEach { slot ->
            val mon = slot.pokemonId?.let { pokeMap[it] }
            val pairs = extractSlotMoveTypesAndCategories(slot, mon, isModern)
            val slotTypes = mutableListOf<PokemonType>()
            pairs.forEach { (t, cat) ->
                slotTypes.add(t)
                if (cat == "Special") specCount++ else physCount++
            }
            perSlotMoves.add(slotTypes)
        }

        val covered = mutableSetOf<PokemonType>()
        val memberHitCounts = mutableMapOf<PokemonType, Int>()
        val totalMoveHits = mutableMapOf<PokemonType, Int>()

        allTypes.forEach { defendingType ->
            // Count how many distinct party Pokémon (0..6) have at least 1 move or Secret Type hitting defendingType for 2x
            val membersCovering = perSlotMoves.count { slotMoveTypes ->
                slotMoveTypes.any { atkType ->
                    PokemonType.getMultiplier(atkType, defendingType, isModern) >= 2.0f
                }
            }
            val totalMoves = perSlotMoves.sumOf { slotMoveTypes ->
                slotMoveTypes.count { atkType ->
                    PokemonType.getMultiplier(atkType, defendingType, isModern) >= 2.0f
                }
            }
            memberHitCounts[defendingType] = membersCovering
            totalMoveHits[defendingType] = totalMoves
            if (membersCovering > 0) {
                covered.add(defendingType)
            }
        }

        val blindSpots = allTypes.filter { !covered.contains(it) }
        return PartyMoveBreakdown(
            coveredTypes = covered,
            blindSpots = blindSpots,
            physicalCount = physCount,
            specialCount = specCount,
            typeHitCounts = memberHitCounts,
            typeTotalMoveHits = totalMoveHits
        )
    }

    fun recommendMoveTypesForBlindSpots(
        blindSpots: List<PokemonType>,
        isModern: Boolean
    ): List<PokemonType> {
        if (blindSpots.isEmpty()) return emptyList()
        val allTypes = PokemonType.activeTypes(isModern)
        return allTypes
            .map { candidateAtk ->
                val coveredCount = blindSpots.count { blindDef ->
                    PokemonType.getMultiplier(candidateAtk, blindDef, isModern) >= 2.0f
                }
                candidateAtk to coveredCount
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    suspend fun analyzeParty(
        party: List<PartySlot>,
        allPokemon: List<Pokemon>,
        isModern: Boolean
    ): TacticalCoachAnalysis = withContext(Dispatchers.Default) {
        val pokeMap = allPokemon.associateBy { it.id }
        val activeSlots = party.filter { it.pokemonId != null }
        val activeMembers = activeSlots.mapNotNull { slot -> slot.pokemonId?.let { pokeMap[it] } }

        if (activeMembers.isEmpty()) {
            return@withContext TacticalCoachAnalysis(
                title = "Party Roster Empty",
                overallRating = "Incomplete (0/10)",
                strengths = listOf("Clean slate ready for team construction."),
                weaknesses = listOf("No Pokémon assigned to any of the 6 battle slots."),
                recommendations = listOf("Add 3 to 6 Pokémon in Party Lab to audit defensive weaknesses and 4-Move + Secret Type coverage."),
                summary = "Select Pokémon in the 6 party slots and configure their 4 Move types, Secret Type, and Physical/Special categories."
            )
        }

        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        val allTypes = PokemonType.activeTypes(isModern)
        val coverage = computeMoveCoverageBreakdown(party, pokeMap, isModern)

        // Defensive vulnerability check
        val sharedWeaknesses = mutableMapOf<PokemonType, Int>()
        allTypes.forEach { attackType ->
            var weakCount = 0
            activeMembers.forEach { mon ->
                val mult = PokemonType.getDefendingMultiplier(attackType, mon.primaryType, mon.secondaryType, isModern)
                if (mult >= 2.0f) weakCount++
            }
            if (weakCount >= 3) {
                sharedWeaknesses[attackType] = weakCount
            }
        }

        // Check for traps
        val trapsInParty = activeMembers.filter { it.isTrap }
        if (trapsInParty.isNotEmpty()) {
            weaknesses.add("Contains Underperforming Trap(s): ${trapsInParty.joinToString { it.name }}. ${trapsInParty.first().trapReason ?: "Suboptimal in competitive play."}")
            recommendations.add("Consider replacing ${trapsInParty.joinToString { it.name }} with higher-consistency offensive or defensive pivots.")
        }

        // Stats balance
        val avgBst = activeMembers.map { it.bst }.average().toInt()
        val fastCount = activeMembers.count { it.speed >= 100 }
        val bulkyCount = activeMembers.count { (it.hp + it.defense + it.spDef) >= 250 }

        if (avgBst >= 500) {
            strengths.add("High Average Base Stat Total (BST $avgBst) provides strong baseline stats.")
        } else {
            weaknesses.add("Modest Average BST ($avgBst) requires careful prediction against OU/Uber threats.")
        }

        if (coverage.coveredTypes.size >= allTypes.size - 2) {
            strengths.add("Superior move coverage: hitting ${coverage.coveredTypes.size}/${allTypes.size} types for Super Effective damage.")
        } else {
            weaknesses.add("Offensive blind spots against: ${coverage.blindSpots.joinToString { it.displayName }}.")
            val suggested = recommendMoveTypesForBlindSpots(coverage.blindSpots, isModern)
            recommendations.add("Adjust a move or Secret Type slot to ${suggested.joinToString(" or ") { it.displayName }} to eliminate offensive blind spots.")
        }

        val totalMoves = coverage.physicalCount + coverage.specialCount
        if (totalMoves > 0) {
            val physPct = (coverage.physicalCount * 100) / totalMoves
            val specPct = 100 - physPct
            if (physPct in 30..70) {
                strengths.add("Balanced Physical/Special split ($physPct% Physical / $specPct% Special) prevents single-wall stalling.")
            } else if (physPct > 70) {
                weaknesses.add("Heavily skewed toward Physical attacks ($physPct% Physical) — vulnerable to Intimidate, Will-O-Wisp, and physical walls.")
                recommendations.add("Switch 2-3 move or Secret Type slots to Special category on high Sp.Atk members.")
            } else {
                weaknesses.add("Heavily skewed toward Special attacks ($specPct% Special) — vulnerable to Blissey/Snorlax special walls.")
                recommendations.add("Include strong Physical coverage (Earthquake, Close Combat, or Stone Edge) to break Special walls.")
            }
        }

        if (fastCount >= 2) {
            strengths.add("Reliable speed control with $fastCount fast attacker(s) (Base Speed >= 100).")
        } else {
            recommendations.add("Add a high-speed sweeper (Base Speed >= 100) or priority user for late-game cleanup.")
        }

        if (bulkyCount >= 1) {
            strengths.add("Defensive backbone present to absorb neutral and resisted hits.")
        }

        if (sharedWeaknesses.isNotEmpty()) {
            val topWeak = sharedWeaknesses.maxByOrNull { it.value }!!
            weaknesses.add("Stacked defensive weakness: ${topWeak.value} team members take 2x/4x damage from ${topWeak.key.displayName}.")
            recommendations.add("Add a ${topWeak.key.displayName} immunity or 0.5x resistor to stabilize switch-ins.")
        } else {
            strengths.add("Clean defensive synergy: zero stacked weaknesses (>=3) across all ${allTypes.size} types.")
        }

        val ratingScore = (6.0 +
                (if (sharedWeaknesses.isEmpty()) 1.5 else -1.0) +
                (if (coverage.blindSpots.size <= 2) 1.5 else -0.5) +
                (if (trapsInParty.isEmpty()) 0.5 else -1.5) +
                (if (activeMembers.size == 6) 0.5 else 0.0)).coerceIn(1.0, 10.0)

        val scoreFormatted = String.format("%.1f", ratingScore)
        val overall = when {
            ratingScore >= 8.5 -> "Tournament Grade ($scoreFormatted/10)"
            ratingScore >= 7.0 -> "Competitive Ready ($scoreFormatted/10)"
            ratingScore >= 5.0 -> "Playable Core ($scoreFormatted/10)"
            else -> "Needs Rebalancing ($scoreFormatted/10)"
        }

        val summary = "Audited ${activeMembers.size}/6 members under ${if (isModern) "Modern (18 Types)" else "Gen 4 HGSS (17 Types)"} mechanics. Coverage: ${coverage.coveredTypes.size}/${allTypes.size} types (${coverage.physicalCount} Phys / ${coverage.specialCount} Spec)."

        TacticalCoachAnalysis(
            title = "Tactical AI Coach Audit",
            overallRating = overall,
            strengths = strengths,
            weaknesses = weaknesses,
            recommendations = recommendations,
            summary = summary
        )
    }

    suspend fun generateInteractiveChatResponse(
        userQuery: String,
        party: List<PartySlot>,
        allPokemon: List<Pokemon>,
        isModern: Boolean
    ): String = withContext(Dispatchers.Default) {
        val pokeMap = allPokemon.associateBy { it.id }
        val activeSlots = party.filter { it.pokemonId != null }
        val activeMembers = activeSlots.mapNotNull { slot -> slot.pokemonId?.let { pokeMap[it] } }
        val allTypes = PokemonType.activeTypes(isModern)

        if (activeMembers.isEmpty()) {
            return@withContext "Tim Anda saat ini masih kosong (0/6 slot terisi). Silakan pilih minimal 1 hingga 6 Pokémon pada kartu slot Party Lab di atas beserta tipe ke-4 Move dan Secret Type-nya, lalu tanyakan kembali strategi yang ingin dianalisis."
        }

        val coverage = computeMoveCoverageBreakdown(party, pokeMap, isModern)

        val defWeakMap = mutableMapOf<PokemonType, MutableList<String>>()
        val def4xList = mutableListOf<String>()
        allTypes.forEach { atkType ->
            activeMembers.forEach { mon ->
                val mult = PokemonType.getDefendingMultiplier(atkType, mon.primaryType, mon.secondaryType, isModern)
                if (mult >= 2.0f) {
                    defWeakMap.getOrPut(atkType) { mutableListOf() }.add(mon.name)
                }
                if (mult >= 4.0f) {
                    def4xList.add("${mon.name} (4x vs ${atkType.displayName})")
                }
            }
        }

        val stackedWeak = defWeakMap.filter { it.value.size >= 3 }
        val q = userQuery.lowercase()

        val rosterSummary = activeSlots.mapNotNull { slot ->
            val mon = slot.pokemonId?.let { pokeMap[it] } ?: return@mapNotNull null
            val pairs = extractSlotMoveTypesAndCategories(slot, mon, isModern)
            val movesLabel = pairs.take(4).joinToString(", ") { "${it.first.displayName} (${it.second.take(4)})" }
            val secLabel = slot.secretType?.let { " | Secret: ${it.displayName} (${slot.secretCategory.take(4)})" } ?: ""
            "- ${mon.name} (${mon.primaryType.displayName}${mon.secondaryType?.let { "/${it.displayName}" } ?: ""}, BST ${mon.bst}): Moves [$movesLabel]$secLabel"
        }.joinToString("\n")

        return@withContext when {
            q.contains("cover") || q.contains("blind") || q.contains("offens") || q.contains("serang") || q.contains("move") || q.contains("secret") || q.contains("tera") -> {
                val totalM = (coverage.physicalCount + coverage.specialCount).coerceAtLeast(1)
                val physPct = (coverage.physicalCount * 100) / totalM
                val specPct = 100 - physPct
                buildString {
                    appendLine("Analisis Offensive Coverage (4 Moves + Secret Type):")
                    appendLine("• Super-Effective Coverage: ${coverage.coveredTypes.size} dari ${allTypes.size} tipe elemen.")
                    appendLine("• Rasio Kategori Serangan: ${coverage.physicalCount} Physical ($physPct%) vs ${coverage.specialCount} Special ($specPct%).")
                    if (coverage.blindSpots.isNotEmpty()) {
                        appendLine("• Blind Spots (0 anggota tim memiliki jurus Super Effective terhadap tipe ini): ${coverage.blindSpots.joinToString(", ") { it.displayName }}.")
                        val neededTypes = recommendMoveTypesForBlindSpots(coverage.blindSpots, isModern)
                        appendLine("• Rekomendasi Taktis: Pasang Move atau Secret Type bertipe ${neededTypes.joinToString(" / ") { it.displayName }} agar cakupan serangan mencapai 100%.")
                    } else {
                        appendLine("• Sempurna! Kombinasi 4 Move + Secret Type tim Anda mampu menghantam seluruh ${allTypes.size} tipe elemen dengan kerusakan 2x Super Effective.")
                    }
                }.trim()
            }

            q.contains("defens") || q.contains("weak") || q.contains("lemah") || q.contains("tahan") -> {
                buildString {
                    appendLine("Audit Ketahanan Defensif Tim (${activeMembers.size} Pokémon):")
                    if (def4xList.isNotEmpty()) {
                        appendLine("• Kerentanan Kritis 4x (Double Weakness): ${def4xList.joinToString("; ")}.")
                    } else {
                        appendLine("• Tidak ada anggota tim yang memiliki kelemahan ganda 4x.")
                    }
                    if (stackedWeak.isNotEmpty()) {
                        stackedWeak.forEach { (type, mons) ->
                            appendLine("• Tumpukan Kelemahan ${type.displayName} (${mons.size} Pokémon): ${mons.joinToString(", ")}.")
                        }
                        val worstType = stackedWeak.maxByOrNull { it.value.size }!!.key
                        val immuneOrResist = allTypes.filter { defT ->
                            PokemonType.getMultiplier(worstType, defT, isModern) <= 0.5f
                        }.joinToString(", ") { it.displayName }
                        appendLine("Solusi Taktis: Tambahkan pivot bertipe $immuneOrResist untuk menyerap serangan ${worstType.displayName} saat melakukan switch-in.")
                    } else {
                        appendLine("• Struktur pertahanan sangat seimbang: tidak ada tipe elemen yang menghantam >= 3 anggota tim sekaligus.")
                    }
                }.trim()
            }

            q.contains("physical") || q.contains("special") || q.contains("kategori") || q.contains("split") -> {
                buildString {
                    appendLine("Audit Keselarasan Stat Attack vs Sp.Atk dengan Kategori Move:")
                    activeSlots.forEach { slot ->
                        val mon = slot.pokemonId?.let { pokeMap[it] } ?: return@forEach
                        val pairs = extractSlotMoveTypesAndCategories(slot, mon, isModern)
                        val pCount = pairs.count { it.second == "Physical" }
                        val sCount = pairs.count { it.second == "Special" }
                        val statBias = when {
                            mon.attack - mon.spAtk >= 20 -> "Physical Attacker (Atk ${mon.attack} > SpA ${mon.spAtk})"
                            mon.spAtk - mon.attack >= 20 -> "Special Attacker (SpA ${mon.spAtk} > Atk ${mon.attack})"
                            else -> "Mixed Attacker (Atk ${mon.attack} / SpA ${mon.spAtk})"
                        }
                        appendLine("• ${mon.name}: $statBias — terpasang $pCount Physical & $sCount Special.")
                    }
                }.trim()
            }

            else -> {
                val totalM = (coverage.physicalCount + coverage.specialCount).coerceAtLeast(1)
                val physPct = (coverage.physicalCount * 100) / totalM
                buildString {
                    appendLine("Laporan Taktis Real-Time (${if (isModern) "Modern 18-Type" else "Gen 4 HGSS 17-Type"}):")
                    appendLine(rosterSummary)
                    appendLine()
                    appendLine("1. Cakupan Serangan: ${coverage.coveredTypes.size}/${allTypes.size} tipe ter-cover (${coverage.physicalCount} Physical / ${coverage.specialCount} Special, $physPct% Phys).")
                    if (coverage.blindSpots.isNotEmpty()) {
                        appendLine("   Blind Spot: ${coverage.blindSpots.joinToString(", ") { it.displayName }}.")
                    } else {
                        appendLine("   Blind Spot: Nihil (100% tipe lawan ter-cover Super Effective).")
                    }
                    if (stackedWeak.isNotEmpty()) {
                        appendLine("2. Peringatan Pertahanan: Tim bertumpuk lemah terhadap ${stackedWeak.entries.joinToString { "${it.key.displayName} (${it.value.size}x)" }}.")
                    } else {
                        appendLine("2. Pertahanan: Solid, tidak ada tumpukan kelemahan >= 3 anggota.")
                    }
                    appendLine("3. Saran: Pastikan tiap anggota memakai kategori Move (Physical/Special) yang sesuai dengan stat tertinggi mereka dan manfaatkan Secret Type (Hidden Power / Tera) untuk menutup celah coverage.")
                }.trim()
            }
        }
    }
}
