package com.example.data

import com.example.data.model.PokemonType
import kotlin.random.Random

/**
 * Represents one of the 171 canonical Pokémon type combinations:
 * - 18 Single-Type combinations (secondary == null)
 * - 153 Dual-Type pairings (18 * 17 / 2)
 */
data class TypeCombo(
    val index: Int,
    val primary: PokemonType,
    val secondary: PokemonType? = null,
    val isUnusedInMainSeries: Boolean = false
) {
    val displayName: String
        get() = if (secondary == null) primary.displayName else "${primary.displayName}/${secondary.displayName}"

    val hasFairy: Boolean
        get() = primary == PokemonType.FAIRY || secondary == PokemonType.FAIRY
}

enum class DrillCategoryFilter(val label: String) {
    ALL("All"),
    ADVANTAGE("Advantage"),
    STAB("STAB"),
    RESISTANCE("Resistance")
}

enum class DrillQuestionCategory(val title: String) {
    ADVANTAGE("Advantage"),
    STAB("STAB"),
    RESISTANCE_RESIST("Resistance (Resist)"),
    RESISTANCE_IMMUNE("Resistance (Immune)")
}

data class GeneratedDrillQuestion(
    val globalNumber: Int,          // 1..3591
    val totalGlobalQuestions: Int = 3591,
    val categoryNumber: Int,        // 1..categoryTotal
    val categoryTotal: Int,
    val category: DrillQuestionCategory,
    val playerCombo: TypeCombo?,
    val opponentCombo: TypeCombo?,
    val neutralCombo: TypeCombo? = null,
    val prompt: String,
    val options: List<String>,
    val optionTypes: List<List<PokemonType>>,
    val correctIndex: Int,
    val calculationBreakdown: String,
    val explanation: String
)

object BattleDrillsEngine {

    /**
     * Exact 6x3 Grid order:
     * Row 1: [Normal] [Fire] [Water] [Electric] [Grass] [Ice]
     * Row 2: [Fighting] [Poison] [Ground] [Flying] [Psychic] [Bug]
     * Row 3: [Rock] [Ghost] [Dragon] [Dark] [Steel] [Fairy]
     */
    val GRID_TYPES_ORDER: List<PokemonType> = listOf(
        PokemonType.NORMAL, PokemonType.FIRE, PokemonType.WATER,
        PokemonType.ELECTRIC, PokemonType.GRASS, PokemonType.ICE,
        PokemonType.FIGHTING, PokemonType.POISON, PokemonType.GROUND,
        PokemonType.FLYING, PokemonType.PSYCHIC, PokemonType.BUG,
        PokemonType.ROCK, PokemonType.GHOST, PokemonType.DRAGON,
        PokemonType.DARK, PokemonType.STEEL, PokemonType.FAIRY
    )

    /**
     * The 9 unused type combinations explicitly highlighted in the specification
     */
    private val UNUSED_TYPE_PAIRINGS: Set<Set<PokemonType>> = setOf(
        setOf(PokemonType.NORMAL, PokemonType.ICE),
        setOf(PokemonType.NORMAL, PokemonType.BUG),
        setOf(PokemonType.NORMAL, PokemonType.ROCK),
        setOf(PokemonType.NORMAL, PokemonType.STEEL),
        setOf(PokemonType.FIRE, PokemonType.FAIRY),
        setOf(PokemonType.ICE, PokemonType.POISON),
        setOf(PokemonType.GROUND, PokemonType.FAIRY),
        setOf(PokemonType.BUG, PokemonType.DRAGON),
        setOf(PokemonType.ROCK, PokemonType.GHOST)
    )

    /**
     * All 171 canonical type combinations:
     * 0..17    -> 18 Single-Type options
     * 18..170  -> 153 Dual-Type pairings
     */
    val ALL_171_COMBOS: List<TypeCombo> by lazy {
        val list = mutableListOf<TypeCombo>()
        var idx = 0
        // 1. 18 Single-type options
        for (t in GRID_TYPES_ORDER) {
            list.add(
                TypeCombo(
                    index = idx++,
                    primary = t,
                    secondary = null,
                    isUnusedInMainSeries = false
                )
            )
        }
        // 2. 153 Dual-type pairings
        for (i in GRID_TYPES_ORDER.indices) {
            for (j in (i + 1) until GRID_TYPES_ORDER.size) {
                val t1 = GRID_TYPES_ORDER[i]
                val t2 = GRID_TYPES_ORDER[j]
                val isUnused = UNUSED_TYPE_PAIRINGS.contains(setOf(t1, t2))
                list.add(
                    TypeCombo(
                        index = idx++,
                        primary = t1,
                        secondary = t2,
                        isUnusedInMainSeries = isUnused
                    )
                )
            }
        }
        list
    }

    /**
     * 18x18 Type Chart Matrix lookup:
     * typeChart[attackingType][defendingType] = multiplier (0f, 0.5f, 1f, 2f)
     */
    fun getMatrixMultiplier(
        attackType: PokemonType,
        defendingType: PokemonType,
        isModern: Boolean
    ): Float {
        return PokemonType.getMultiplier(attackType, defendingType, isModern)
    }

    /**
     * Calculates effectiveness of a single attacking type against a 1- or 2-type defender.
     */
    fun calculateEffectiveness(
        attackType: PokemonType,
        defenderType1: PokemonType,
        defenderType2: PokemonType? = null,
        isModern: Boolean = true
    ): Float {
        var multiplier = getMatrixMultiplier(attackType, defenderType1, isModern)
        if (defenderType2 != null && defenderType2 != defenderType1) {
            multiplier *= getMatrixMultiplier(attackType, defenderType2, isModern)
        }
        return multiplier
    }

    /**
     * Explains the exact per-type cause of a multiplier against a single or dual-type defender.
     * Instead of vaguely saying "A/B is weak to F", explicitly states WHICH type (A, B, or both)
     * causes the weakness, resistance, cancellation, or immunity.
     */
    fun explainAttackVsCombo(
        attackType: PokemonType,
        defender: TypeCombo,
        isModern: Boolean
    ): String {
        val t1 = defender.primary
        val t2 = defender.secondary
        val m1 = getMatrixMultiplier(attackType, t1, isModern)
        if (t2 == null) {
            return when (m1) {
                2.0f -> "${attackType.displayName} is super effective (2×) against ${t1.displayName}"
                0.5f -> "${t1.displayName} resists ${attackType.displayName} (0.5×)"
                0.0f -> "${t1.displayName} is completely immune (0×) to ${attackType.displayName}"
                else -> "${attackType.displayName} deals neutral damage (1×) to ${t1.displayName}"
            }
        }

        val m2 = getMatrixMultiplier(attackType, t2, isModern)
        val total = m1 * m2
        return when {
            total == 4.0f ->
                "${attackType.displayName} deals 4× because BOTH ${t1.displayName} (2×) and ${t2.displayName} (2×) are weak to ${attackType.displayName}"
            total == 2.0f && m1 == 2.0f ->
                "${attackType.displayName} deals 2× because ${t1.displayName} is weak to ${attackType.displayName} (2×), while ${t2.displayName} takes neutral 1×"
            total == 2.0f && m2 == 2.0f ->
                "${attackType.displayName} deals 2× because ${t2.displayName} is weak to ${attackType.displayName} (2×), while ${t1.displayName} takes neutral 1×"
            total == 1.0f && m1 == 2.0f && m2 == 0.5f ->
                "${attackType.displayName} deals 1× (neutral) because ${t1.displayName}'s weakness (2×) is canceled by ${t2.displayName}'s resistance (0.5×)"
            total == 1.0f && m2 == 2.0f && m1 == 0.5f ->
                "${attackType.displayName} deals 1× (neutral) because ${t2.displayName}'s weakness (2×) is canceled by ${t1.displayName}'s resistance (0.5×)"
            total == 1.0f ->
                "${attackType.displayName} deals 1× (neutral) against both ${t1.displayName} and ${t2.displayName}"
            total == 0.5f && m1 == 0.5f ->
                "${attackType.displayName} deals 0.5× because ${t1.displayName} resists ${attackType.displayName} (0.5×), while ${t2.displayName} takes neutral 1×"
            total == 0.5f && m2 == 0.5f ->
                "${attackType.displayName} deals 0.5× because ${t2.displayName} resists ${attackType.displayName} (0.5×), while ${t1.displayName} takes neutral 1×"
            total == 0.25f ->
                "${attackType.displayName} deals 0.25× because BOTH ${t1.displayName} (0.5×) and ${t2.displayName} (0.5×) resist ${attackType.displayName}"
            total == 0.0f && m1 == 0.0f && m2 == 0.0f ->
                "${attackType.displayName} deals 0× because both ${t1.displayName} and ${t2.displayName} are immune to ${attackType.displayName}"
            total == 0.0f && m1 == 0.0f ->
                "${attackType.displayName} deals 0× because ${t1.displayName} is completely immune (0×) to ${attackType.displayName}"
            total == 0.0f && m2 == 0.0f ->
                "${attackType.displayName} deals 0× because ${t2.displayName} is completely immune (0×) to ${attackType.displayName}"
            else ->
                "${attackType.displayName} deals ${formatMult(total)}× against ${defender.displayName}"
        }
    }

    /**
     * Checks whether a global question index (0 until 3591) involves the Fairy type.
     * When Gen 4 mode (!isModern) is active, Fairy questions are completely excluded.
     */
    fun involvesFairy(globalIndex: Int): Boolean {
        val safeIndex = globalIndex.coerceIn(0, 3590)
        return when {
            safeIndex < 3078 -> {
                val playerTypeIndex = safeIndex / 171
                val opponentComboIndex = safeIndex % 171
                GRID_TYPES_ORDER[playerTypeIndex] == PokemonType.FAIRY ||
                        ALL_171_COMBOS[opponentComboIndex].hasFairy
            }
            safeIndex < 3249 -> ALL_171_COMBOS[safeIndex - 3078].hasFairy
            safeIndex < 3420 -> ALL_171_COMBOS[safeIndex - 3249].hasFairy
            else -> ALL_171_COMBOS[safeIndex - 3420].hasFairy
        }
    }

    /**
     * Returns all raw global question indices (0 until 3591) for the selected filter.
     * - 0 until 3078   : Category 1 - Advantage (18 * 171 = 3,078)
     * - 3078 until 3249: Category 2 - STAB (171)
     * - 3249 until 3420: Category 3A - Resistance Resist <= 0.5x (171)
     * - 3420 until 3591: Category 3B - Resistance Immune == 0x (171)
     */
    fun getIndicesForFilter(filter: DrillCategoryFilter): IntRange {
        return when (filter) {
            DrillCategoryFilter.ALL -> 0 until 3591
            DrillCategoryFilter.ADVANTAGE -> 0 until 3078
            DrillCategoryFilter.STAB -> 3078 until 3249
            DrillCategoryFilter.RESISTANCE -> 3249 until 3591
        }
    }

    /**
     * Returns valid global indices filtered by category AND Gen 4 / Modern mode.
     * In Gen 4 mode (!isModern), any question involving Fairy type is filtered out.
     */
    fun getValidGlobalIndices(filter: DrillCategoryFilter, isModern: Boolean): List<Int> {
        val baseRange = getIndicesForFilter(filter)
        return if (isModern) {
            baseRange.toList()
        } else {
            baseRange.filter { !involvesFairy(it) }
        }
    }

    /**
     * Dynamically generates any of the 3,591 questions on demand from its global index (0..3590).
     */
    fun generateQuestionByGlobalIndex(
        globalIndex: Int,
        isModern: Boolean
    ): GeneratedDrillQuestion {
        val safeIndex = globalIndex.coerceIn(0, 3590)
        return when {
            safeIndex < 3078 -> generateAdvantageQuestion(safeIndex, isModern)
            safeIndex < 3249 -> generateStabQuestion(safeIndex - 3078, safeIndex, isModern)
            safeIndex < 3420 -> generateResistanceResistQuestion(safeIndex - 3249, safeIndex, isModern)
            else -> generateResistanceImmuneQuestion(safeIndex - 3420, safeIndex, isModern)
        }
    }

    /**
     * Category 1: Advantage Drill (replaces Tactical "Stay or Switch?")
     * Asks who has the type advantage between Your Pokémon and Opponent's Pokémon.
     * Provides 3 plain-text options (no colorful TypePill panels):
     *   0 -> Your Pokémon has the advantage
     *   1 -> Equal (Balanced matchup)
     *   2 -> Opponent's Pokémon has the advantage
     */
    private fun generateAdvantageQuestion(
        tacticalIndex: Int,
        isModern: Boolean
    ): GeneratedDrillQuestion {
        val playerTypeIndex = tacticalIndex / 171
        val opponentComboIndex = tacticalIndex % 171

        val playerType = GRID_TYPES_ORDER[playerTypeIndex]
        val playerCombo = ALL_171_COMBOS[playerTypeIndex] // Single type (0..17)
        val opponentCombo = ALL_171_COMBOS[opponentComboIndex]

        val playerAttackEff = calculateEffectiveness(
            attackType = playerType,
            defenderType1 = opponentCombo.primary,
            defenderType2 = opponentCombo.secondary,
            isModern = isModern
        )

        val oppEff1 = calculateEffectiveness(
            attackType = opponentCombo.primary,
            defenderType1 = playerType,
            defenderType2 = null,
            isModern = isModern
        )
        val oppEff2 = opponentCombo.secondary?.let {
            calculateEffectiveness(
                attackType = it,
                defenderType1 = playerType,
                defenderType2 = null,
                isModern = isModern
            )
        } ?: -1f

        val opponentBestEff = maxOf(oppEff1, oppEff2)
        val opponentBestType = if (opponentCombo.secondary != null && oppEff2 > oppEff1) {
            opponentCombo.secondary
        } else {
            opponentCombo.primary
        }

        // 3 Options: 0 = Player Advantage, 1 = Equal, 2 = Opponent Advantage
        val correctIdx = when {
            playerAttackEff > opponentBestEff -> 0
            playerAttackEff == opponentBestEff -> 1
            else -> 2
        }

        val breakdown = "Your ${playerType.displayName} → ${opponentCombo.displayName}: ${formatMult(playerAttackEff)}×  |  " +
                "Opponent ${opponentBestType.displayName} → ${playerType.displayName}: ${formatMult(opponentBestEff)}×"

        val playerCause = explainAttackVsCombo(playerType, opponentCombo, isModern)
        val oppCause = explainAttackVsCombo(opponentBestType, playerCombo, isModern)

        val explanation = when (correctIdx) {
            0 -> "Your Pokémon has the advantage (${formatMult(playerAttackEff)}× vs ${formatMult(opponentBestEff)}×): $playerCause, whereas $oppCause."
            1 -> "The matchup is Equal (${formatMult(playerAttackEff)}× vs ${formatMult(opponentBestEff)}×): $playerCause, and in return $oppCause."
            else -> "Opponent has the advantage (${formatMult(opponentBestEff)}× vs ${formatMult(playerAttackEff)}×): $oppCause, whereas $playerCause."
        }

        return GeneratedDrillQuestion(
            globalNumber = tacticalIndex + 1,
            categoryNumber = tacticalIndex + 1,
            categoryTotal = 3078,
            category = DrillQuestionCategory.ADVANTAGE,
            playerCombo = playerCombo,
            opponentCombo = opponentCombo,
            neutralCombo = null,
            prompt = "Your Pokémon is ${playerType.displayName} type and the opponent is ${opponentCombo.displayName} type. Who has the type advantage?",
            options = listOf(
                "Your Pokémon has the advantage",
                "Equal (Balanced matchup)",
                "Opponent has the advantage"
            ),
            // Plain text ONLY for Advantage options (no colorful TypePill panels)
            optionTypes = listOf(emptyList(), emptyList(), emptyList()),
            correctIndex = correctIdx,
            calculationBreakdown = breakdown,
            explanation = explanation
        )
    }

    /**
     * Category 2: STAB Drill (replaces Counter "Attack with What?")
     * Asks which attack types deal super-effective damage against an opponent's type combo.
     * Options use colorful TypePill panels ONLY.
     */
    private fun generateStabQuestion(
        comboIndex: Int,
        globalIndex: Int,
        isModern: Boolean
    ): GeneratedDrillQuestion {
        val opponentCombo = ALL_171_COMBOS[comboIndex]
        val rng = Random(globalIndex * 9973 + if (isModern) 1 else 2)

        val activeTypes = PokemonType.activeTypes(isModern)
        val superEffectiveTypes = activeTypes.filter { atk ->
            calculateEffectiveness(atk, opponentCombo.primary, opponentCombo.secondary, isModern) >= 2.0f
        }
        val fourXTypes = activeTypes.filter { atk ->
            calculateEffectiveness(atk, opponentCombo.primary, opponentCombo.secondary, isModern) >= 4.0f
        }
        val nonSuperTypes = activeTypes.filter { atk ->
            calculateEffectiveness(atk, opponentCombo.primary, opponentCombo.secondary, isModern) < 2.0f
        }

        val prioritized = if (fourXTypes.isNotEmpty()) {
            fourXTypes + (superEffectiveTypes - fourXTypes.toSet())
        } else {
            superEffectiveTypes
        }
        val correctTypes: List<PokemonType> = prioritized.take(3)

        val cleanCorrectLabel = if (correctTypes.isEmpty()) {
            "None (No 2× Weaknesses)"
        } else {
            correctTypes.joinToString(", ") { it.displayName }
        }

        val distractorSets = mutableListOf<Pair<String, List<PokemonType>>>()
        val shuffledWrong = nonSuperTypes.shuffled(rng)
        val groupSize = correctTypes.size.coerceIn(1, 2)

        var cursor = 0
        while (distractorSets.size < 3 && cursor < shuffledWrong.size) {
            val slice = shuffledWrong.subList(cursor, minOf(cursor + groupSize, shuffledWrong.size))
            cursor += groupSize
            val label = slice.joinToString(", ") { it.displayName }
            distractorSets.add(label to slice)
        }

        val allChoices = (distractorSets + (cleanCorrectLabel to correctTypes)).shuffled(rng)
        val correctIdx = allChoices.indexOfFirst { it.first == cleanCorrectLabel }

        val fullSuperListStr = if (superEffectiveTypes.isEmpty()) {
            "None (0 types deal ≥2×)"
        } else {
            superEffectiveTypes.joinToString(", ") {
                val m = calculateEffectiveness(it, opponentCombo.primary, opponentCombo.secondary, isModern)
                "${it.displayName} (${formatMult(m)}×)"
            }
        }

        val detailedExplanation = if (correctTypes.isEmpty()) {
            if (opponentCombo.secondary != null) {
                "Every weakness of ${opponentCombo.primary.displayName} is resisted or immune by ${opponentCombo.secondary.displayName} (and vice versa), leaving 0 weaknesses in ${if (isModern) "Modern" else "Gen 4"}."
            } else {
                "${opponentCombo.displayName} has no 2× weaknesses."
            }
        } else {
            correctTypes.joinToString(". ") { atk ->
                explainAttackVsCombo(atk, opponentCombo, isModern)
            } + "."
        }

        return GeneratedDrillQuestion(
            globalNumber = globalIndex + 1,
            categoryNumber = comboIndex + 1,
            categoryTotal = 171,
            category = DrillQuestionCategory.STAB,
            playerCombo = null,
            opponentCombo = opponentCombo,
            neutralCombo = null,
            prompt = "Opponent's Pokémon is ${opponentCombo.displayName} type. Which attack types deal super effective damage?",
            options = allChoices.map { it.first },
            optionTypes = allChoices.map { it.second },
            correctIndex = correctIdx,
            calculationBreakdown = "All ≥2× STAB Counters vs ${opponentCombo.displayName}: $fullSuperListStr",
            explanation = detailedExplanation
        )
    }

    /**
     * Category 3A: Resistance Drill - Resist
     * Neutral phrasing: "If a Pokémon is {Type} type, which attack types are resisted (0.5× or less)?"
     */
    private fun generateResistanceResistQuestion(
        comboIndex: Int,
        globalIndex: Int,
        isModern: Boolean
    ): GeneratedDrillQuestion {
        val targetCombo = ALL_171_COMBOS[comboIndex]
        val rng = Random(globalIndex * 7919 + if (isModern) 11 else 23)
        val activeTypes = PokemonType.activeTypes(isModern)

        val resistedTypes = activeTypes.filter { atk ->
            val m = calculateEffectiveness(atk, targetCombo.primary, targetCombo.secondary, isModern)
            m in 0.1f..0.5f
        }
        val nonResistedTypes = activeTypes.filter { atk ->
            calculateEffectiveness(atk, targetCombo.primary, targetCombo.secondary, isModern) >= 1.0f
        }

        val chosenResists = resistedTypes.sortedBy {
            calculateEffectiveness(it, targetCombo.primary, targetCombo.secondary, isModern)
        }.take(3)

        val cleanCorrectLabel = if (chosenResists.isEmpty()) {
            "None (No 0.5×/0.25× Resistances)"
        } else {
            chosenResists.joinToString(", ") { it.displayName }
        }

        val distractorSets = mutableListOf<Pair<String, List<PokemonType>>>()
        val shuffledWrong = nonResistedTypes.shuffled(rng)
        val groupSize = chosenResists.size.coerceIn(1, 2)

        var cursor = 0
        while (distractorSets.size < 3 && cursor < shuffledWrong.size) {
            val slice = shuffledWrong.subList(cursor, minOf(cursor + groupSize, shuffledWrong.size))
            cursor += groupSize
            distractorSets.add(slice.joinToString(", ") { it.displayName } to slice)
        }

        val allChoices = (distractorSets + (cleanCorrectLabel to chosenResists)).shuffled(rng)
        val correctIdx = allChoices.indexOfFirst { it.first == cleanCorrectLabel }

        val fullResistSummary = if (resistedTypes.isEmpty()) {
            "None"
        } else {
            resistedTypes.joinToString(", ") {
                val m = calculateEffectiveness(it, targetCombo.primary, targetCombo.secondary, isModern)
                "${it.displayName} (${formatMult(m)}×)"
            }
        }

        val detailedExplanation = if (chosenResists.isEmpty()) {
            "${targetCombo.displayName} does not resist any attacking types for 0.5× or 0.25× damage."
        } else {
            chosenResists.joinToString(". ") { atk ->
                explainAttackVsCombo(atk, targetCombo, isModern)
            } + "."
        }

        return GeneratedDrillQuestion(
            globalNumber = globalIndex + 1,
            categoryNumber = comboIndex + 1,
            categoryTotal = 342,
            category = DrillQuestionCategory.RESISTANCE_RESIST,
            playerCombo = null,
            opponentCombo = null,
            neutralCombo = targetCombo,
            prompt = "If a Pokémon is ${targetCombo.displayName} type, which attack types are resisted (0.5× or less)?",
            options = allChoices.map { it.first },
            optionTypes = allChoices.map { it.second },
            correctIndex = correctIdx,
            calculationBreakdown = "${targetCombo.displayName} Resistances (≤0.5×): $fullResistSummary",
            explanation = detailedExplanation
        )
    }

    /**
     * Category 3B: Resistance Drill - Immune
     * Neutral phrasing: "If a Pokémon is {Type} type, which attack types deal NO damage (0× Immune)?"
     */
    private fun generateResistanceImmuneQuestion(
        comboIndex: Int,
        globalIndex: Int,
        isModern: Boolean
    ): GeneratedDrillQuestion {
        val targetCombo = ALL_171_COMBOS[comboIndex]
        val rng = Random(globalIndex * 6151 + if (isModern) 31 else 47)
        val activeTypes = PokemonType.activeTypes(isModern)

        val immuneTypes = activeTypes.filter { atk ->
            calculateEffectiveness(atk, targetCombo.primary, targetCombo.secondary, isModern) == 0.0f
        }
        val damagingTypes = activeTypes.filter { atk ->
            calculateEffectiveness(atk, targetCombo.primary, targetCombo.secondary, isModern) > 0.0f
        }

        val cleanCorrectLabel = if (immuneTypes.isEmpty()) {
            "None (No 0× Immunities)"
        } else {
            immuneTypes.joinToString(", ") { it.displayName }
        }

        val distractorSets = mutableListOf<Pair<String, List<PokemonType>>>()
        val shuffledWrong = damagingTypes.shuffled(rng)
        val groupSize = immuneTypes.size.coerceIn(1, 2)

        var cursor = 0
        while (distractorSets.size < 3 && cursor < shuffledWrong.size) {
            val slice = shuffledWrong.subList(cursor, minOf(cursor + groupSize, shuffledWrong.size))
            cursor += groupSize
            distractorSets.add(slice.joinToString(", ") { it.displayName } to slice)
        }
        if (immuneTypes.isNotEmpty() && distractorSets.isNotEmpty()) {
            distractorSets[0] = "None (No 0× Immunities)" to emptyList()
        }

        val allChoices = (distractorSets.take(3) + (cleanCorrectLabel to immuneTypes)).shuffled(rng)
        val correctIdx = allChoices.indexOfFirst { it.first == cleanCorrectLabel }

        val fullImmuneSummary = if (immuneTypes.isEmpty()) {
            "0× Immune: None"
        } else {
            "0× Immune: ${immuneTypes.joinToString(", ") { "${it.displayName} (0×)" }}"
        }

        val detailedExplanation = if (immuneTypes.isEmpty()) {
            if (targetCombo.secondary != null) {
                "Neither ${targetCombo.primary.displayName} nor ${targetCombo.secondary.displayName} has any 0× type immunities."
            } else {
                "${targetCombo.primary.displayName} has no 0× type immunities."
            }
        } else {
            immuneTypes.joinToString(". ") { atk ->
                explainAttackVsCombo(atk, targetCombo, isModern)
            } + "."
        }

        return GeneratedDrillQuestion(
            globalNumber = globalIndex + 1,
            categoryNumber = 171 + comboIndex + 1,
            categoryTotal = 342,
            category = DrillQuestionCategory.RESISTANCE_IMMUNE,
            playerCombo = null,
            opponentCombo = null,
            neutralCombo = targetCombo,
            prompt = "If a Pokémon is ${targetCombo.displayName} type, which attack types deal NO damage (0× Immune)?",
            options = allChoices.map { it.first },
            optionTypes = allChoices.map { it.second },
            correctIndex = correctIdx,
            calculationBreakdown = "${targetCombo.displayName} Immunities: $fullImmuneSummary",
            explanation = detailedExplanation
        )
    }

    fun formatMult(mult: Float): String {
        return when (mult) {
            0.0f -> "0"
            0.25f -> "0.25"
            0.5f -> "0.5"
            1.0f -> "1"
            2.0f -> "2"
            4.0f -> "4"
            else -> mult.toString()
        }
    }
}
