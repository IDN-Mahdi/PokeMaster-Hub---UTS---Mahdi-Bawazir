package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class PokemonType(
    val displayName: String,
    val shortCode: String,
    val color: Color,
    val isModernOnly: Boolean = false // Fairy is modern (Gen 6+)
) {
    NORMAL("Normal", "NRM", Color(0xFFA8A878)),
    FIRE("Fire", "FIR", Color(0xFFF08030)),
    WATER("Water", "WTR", Color(0xFF6890F0)),
    GRASS("Grass", "GRS", Color(0xFF78C850)),
    ELECTRIC("Electric", "ELC", Color(0xFFF8D030)),
    ICE("Ice", "ICE", Color(0xFF98D8D8)),
    FIGHTING("Fighting", "FGT", Color(0xFFC03028)),
    POISON("Poison", "PSN", Color(0xFFA040A0)),
    GROUND("Ground", "GND", Color(0xFFE0C068)),
    FLYING("Flying", "FLY", Color(0xFFA890F0)),
    PSYCHIC("Psychic", "PSY", Color(0xFFF85888)),
    BUG("Bug", "BUG", Color(0xFFA8B820)),
    ROCK("Rock", "RCK", Color(0xFFB8A038)),
    GHOST("Ghost", "GHS", Color(0xFF705898)),
    DRAGON("Dragon", "DRG", Color(0xFF7038F8)),
    STEEL("Steel", "STL", Color(0xFFB8B8D0)),
    DARK("Dark", "DRK", Color(0xFF705848)),
    FAIRY("Fairy", "FRY", Color(0xFFEE99AC), isModernOnly = true);

    companion object {
        fun fromString(name: String?): PokemonType? {
            if (name == null) return null
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
        }

        // Available types based on mechanic mode: Gen 4 (17 types) vs Modern (18 types)
        fun activeTypes(isModern: Boolean): List<PokemonType> {
            return if (isModern) entries else entries.filter { !it.isModernOnly }
        }

        // Returns multiplier for attackingType against defendingType
        fun getMultiplier(attacking: PokemonType, defending: PokemonType, isModern: Boolean): Float {
            if (!isModern && (attacking == FAIRY || defending == FAIRY)) {
                return 1.0f // Fairy did not exist in Gen 4
            }

            // Gen 4 quirk: Steel resisted Dark and Ghost; in Gen 6+ Steel lost those resistances
            if (!isModern && defending == STEEL && (attacking == DARK || attacking == GHOST)) {
                return 0.5f
            }

            return when (attacking) {
                NORMAL -> when (defending) {
                    ROCK, STEEL -> 0.5f
                    GHOST -> 0f
                    else -> 1f
                }
                FIRE -> when (defending) {
                    GRASS, ICE, BUG, STEEL -> 2f
                    FIRE, WATER, ROCK, DRAGON -> 0.5f
                    else -> 1f
                }
                WATER -> when (defending) {
                    FIRE, GROUND, ROCK -> 2f
                    WATER, GRASS, DRAGON -> 0.5f
                    else -> 1f
                }
                GRASS -> when (defending) {
                    WATER, GROUND, ROCK -> 2f
                    FIRE, GRASS, POISON, FLYING, BUG, DRAGON, STEEL -> 0.5f
                    else -> 1f
                }
                ELECTRIC -> when (defending) {
                    WATER, FLYING -> 2f
                    ELECTRIC, GRASS, DRAGON -> 0.5f
                    GROUND -> 0f
                    else -> 1f
                }
                ICE -> when (defending) {
                    GRASS, GROUND, FLYING, DRAGON -> 2f
                    FIRE, WATER, ICE, STEEL -> 0.5f
                    else -> 1f
                }
                FIGHTING -> when (defending) {
                    NORMAL, ICE, ROCK, DARK, STEEL -> 2f
                    POISON, FLYING, PSYCHIC, BUG, FAIRY -> 0.5f
                    GHOST -> 0f
                    else -> 1f
                }
                POISON -> when (defending) {
                    GRASS, FAIRY -> 2f
                    POISON, GROUND, ROCK, GHOST -> 0.5f
                    STEEL -> 0f
                    else -> 1f
                }
                GROUND -> when (defending) {
                    FIRE, ELECTRIC, POISON, ROCK, STEEL -> 2f
                    GRASS, BUG -> 0.5f
                    FLYING -> 0f
                    else -> 1f
                }
                FLYING -> when (defending) {
                    GRASS, FIGHTING, BUG -> 2f
                    ELECTRIC, ROCK, STEEL -> 0.5f
                    else -> 1f
                }
                PSYCHIC -> when (defending) {
                    FIGHTING, POISON -> 2f
                    PSYCHIC, STEEL -> 0.5f
                    DARK -> 0f
                    else -> 1f
                }
                BUG -> when (defending) {
                    GRASS, PSYCHIC, DARK -> 2f
                    FIRE, FIGHTING, POISON, FLYING, GHOST, STEEL, FAIRY -> 0.5f
                    else -> 1f
                }
                ROCK -> when (defending) {
                    FIRE, ICE, FLYING, BUG -> 2f
                    FIGHTING, GROUND, STEEL -> 0.5f
                    else -> 1f
                }
                GHOST -> when (defending) {
                    PSYCHIC, GHOST -> 2f
                    DARK -> 0.5f
                    NORMAL -> 0f
                    STEEL -> if (!isModern) 0.5f else 1f
                    else -> 1f
                }
                DRAGON -> when (defending) {
                    DRAGON -> 2f
                    STEEL -> 0.5f
                    FAIRY -> if (isModern) 0f else 1f
                    else -> 1f
                }
                STEEL -> when (defending) {
                    ICE, ROCK, FAIRY -> 2f
                    FIRE, WATER, ELECTRIC, STEEL -> 0.5f
                    else -> 1f
                }
                DARK -> when (defending) {
                    PSYCHIC, GHOST -> 2f
                    FIGHTING, DARK, FAIRY -> 0.5f
                    STEEL -> if (!isModern) 0.5f else 1f
                    else -> 1f
                }
                FAIRY -> when (defending) {
                    FIGHTING, DRAGON, DARK -> 2f
                    FIRE, POISON, STEEL -> 0.5f
                    else -> 1f
                }
            }
        }

        // Dual defending type calculation
        fun getDefendingMultiplier(
            attacking: PokemonType,
            defendingPrimary: PokemonType,
            defendingSecondary: PokemonType?,
            isModern: Boolean
        ): Float {
            val mult1 = getMultiplier(attacking, defendingPrimary, isModern)
            val mult2 = defendingSecondary?.let { getMultiplier(attacking, it, isModern) } ?: 1.0f
            return mult1 * mult2
        }
    }
}
