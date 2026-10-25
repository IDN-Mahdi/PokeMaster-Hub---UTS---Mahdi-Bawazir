package com.example.data.model

enum class MoveCategory(val displayName: String) {
    PHYSICAL("Physical"),
    SPECIAL("Special"),
    STATUS("Status")
}

data class PokemonMove(
    val name: String,
    val type: PokemonType,
    val category: MoveCategory,
    val power: Int? = null,
    val accuracy: Int? = 100,
    val pp: Int = 15,
    val description: String = ""
)

object MovesDatabase {
    val allMoves = listOf(
        // Normal
        PokemonMove("Extreme Speed", PokemonType.NORMAL, MoveCategory.PHYSICAL, 80, 100, 5, "Strikes first with blinding speed (Priority +2)."),
        PokemonMove("Body Slam", PokemonType.NORMAL, MoveCategory.PHYSICAL, 85, 100, 15, "Has a 30% chance to paralyze the target."),
        PokemonMove("Protect", PokemonType.NORMAL, MoveCategory.STATUS, null, null, 10, "Completely blocks all attacks for one turn."),
        PokemonMove("Swords Dance", PokemonType.NORMAL, MoveCategory.STATUS, null, null, 20, "Sharply boosts user's Attack by 2 stages."),
        PokemonMove("Recover", PokemonType.NORMAL, MoveCategory.STATUS, null, null, 10, "Restores up to 50% of the user's max HP."),

        // Fire
        PokemonMove("Flamethrower", PokemonType.FIRE, MoveCategory.SPECIAL, 90, 100, 15, "Blasts intense fire; 10% burn chance."),
        PokemonMove("Fire Blast", PokemonType.FIRE, MoveCategory.SPECIAL, 110, 85, 5, "Unleashes fierce fire; 10% burn chance."),
        PokemonMove("Flare Blitz", PokemonType.FIRE, MoveCategory.PHYSICAL, 120, 100, 15, "Cloaks user in fire; 33% recoil damage."),
        PokemonMove("Fire Punch", PokemonType.FIRE, MoveCategory.PHYSICAL, 75, 100, 15, "Fiery punch with 10% burn chance."),

        // Water
        PokemonMove("Surf", PokemonType.WATER, MoveCategory.SPECIAL, 90, 100, 15, "Hits target with a giant surging wave."),
        PokemonMove("Hydro Pump", PokemonType.WATER, MoveCategory.SPECIAL, 110, 80, 5, "Blasts a huge volume of water at high pressure."),
        PokemonMove("Scald", PokemonType.WATER, MoveCategory.SPECIAL, 80, 100, 15, "Boiling water attack; 30% chance to burn target."),
        PokemonMove("Waterfall", PokemonType.WATER, MoveCategory.PHYSICAL, 80, 100, 15, "Charges up waterfalls; 20% flinch chance."),

        // Grass
        PokemonMove("Energy Ball", PokemonType.GRASS, MoveCategory.SPECIAL, 90, 100, 10, "Draws power from nature; 10% chance to lower SpD."),
        PokemonMove("Wood Hammer", PokemonType.GRASS, MoveCategory.PHYSICAL, 120, 100, 15, "Slams with rugged body; 33% recoil damage."),
        PokemonMove("Giga Drain", PokemonType.GRASS, MoveCategory.SPECIAL, 75, 100, 10, "Recovers HP equal to 50% of damage dealt."),

        // Electric
        PokemonMove("Thunderbolt", PokemonType.ELECTRIC, MoveCategory.SPECIAL, 90, 100, 15, "Strong electrical blast; 10% paralysis chance."),
        PokemonMove("Volt Switch", PokemonType.ELECTRIC, MoveCategory.SPECIAL, 70, 100, 20, "Attacks and immediately switches user out."),
        PokemonMove("Wild Charge", PokemonType.ELECTRIC, MoveCategory.PHYSICAL, 90, 100, 15, "Shrouds in electricity and tackles; recoil damage."),

        // Ice
        PokemonMove("Ice Beam", PokemonType.ICE, MoveCategory.SPECIAL, 90, 100, 10, "Fires freezing beam; 10% chance to freeze."),
        PokemonMove("Blizzard", PokemonType.ICE, MoveCategory.SPECIAL, 110, 70, 5, "Howling blizzard attack; 100% accurate in Snow/Hail."),
        PokemonMove("Ice Spinner", PokemonType.ICE, MoveCategory.PHYSICAL, 80, 100, 15, "Spins with ice; destroys terrain on field."),

        // Fighting
        PokemonMove("Close Combat", PokemonType.FIGHTING, MoveCategory.PHYSICAL, 120, 100, 5, "All-out assault; lowers user's Def and SpD by 1."),
        PokemonMove("Aura Sphere", PokemonType.FIGHTING, MoveCategory.SPECIAL, 80, null, 20, "Emits aura blast; never misses."),
        PokemonMove("Drain Punch", PokemonType.FIGHTING, MoveCategory.PHYSICAL, 75, 100, 10, "Punches and restores 50% of damage dealt."),

        // Poison
        PokemonMove("Sludge Bomb", PokemonType.POISON, MoveCategory.SPECIAL, 90, 100, 10, "Hurls unsanitary sludge; 30% poison chance."),
        PokemonMove("Gunk Shot", PokemonType.POISON, MoveCategory.PHYSICAL, 120, 80, 5, "Shoots filthy garbage; 30% poison chance."),
        PokemonMove("Toxic", PokemonType.POISON, MoveCategory.STATUS, null, 90, 10, "Badly poisons the target with compounding damage."),

        // Ground
        PokemonMove("Earthquake", PokemonType.GROUND, MoveCategory.PHYSICAL, 100, 100, 10, "Rips open the ground; powerhouse standard."),
        PokemonMove("Earth Power", PokemonType.GROUND, MoveCategory.SPECIAL, 90, 100, 10, "Erupts earth beneath target; 10% chance to lower SpD."),
        PokemonMove("Stealth Rock", PokemonType.ROCK, MoveCategory.STATUS, null, null, 20, "Sets floating stones that damage entering foes."),

        // Flying
        PokemonMove("Brave Bird", PokemonType.FLYING, MoveCategory.PHYSICAL, 120, 100, 15, "Tucks wings and charges; 33% recoil damage."),
        PokemonMove("Hurricane", PokemonType.FLYING, MoveCategory.SPECIAL, 110, 70, 10, "Wraps in fierce wind; 30% confusion chance; 100% in Rain."),
        PokemonMove("Aeroblast", PokemonType.FLYING, MoveCategory.SPECIAL, 100, 95, 5, "Lugia signature; high critical hit ratio."),
        PokemonMove("Roost", PokemonType.FLYING, MoveCategory.STATUS, null, null, 10, "Lands and restores 50% max HP."),

        // Psychic
        PokemonMove("Psychic", PokemonType.PSYCHIC, MoveCategory.SPECIAL, 90, 100, 10, "Strong telekinetic force; 10% SpD drop chance."),
        PokemonMove("Psyshock", PokemonType.PSYCHIC, MoveCategory.SPECIAL, 80, 100, 10, "Deals physical defense damage using Special Attack."),
        PokemonMove("Calm Mind", PokemonType.PSYCHIC, MoveCategory.STATUS, null, null, 20, "Focuses mind; boosts SpAtk and SpDef by 1 stage."),

        // Bug
        PokemonMove("Bug Buzz", PokemonType.BUG, MoveCategory.SPECIAL, 90, 100, 10, "Vibrates sound waves; passes through substitutes."),
        PokemonMove("U-turn", PokemonType.BUG, MoveCategory.PHYSICAL, 70, 100, 20, "Attacks target and immediately switches out."),

        // Rock
        PokemonMove("Stone Edge", PokemonType.ROCK, MoveCategory.PHYSICAL, 100, 80, 5, "Stabs with sharp stones; high critical hit ratio."),
        PokemonMove("Power Gem", PokemonType.ROCK, MoveCategory.SPECIAL, 80, 100, 20, "Sparkles rays of gem light at target."),

        // Ghost
        PokemonMove("Shadow Ball", PokemonType.GHOST, MoveCategory.SPECIAL, 80, 100, 15, "Hurls shadowy blob; 20% chance to lower SpD."),
        PokemonMove("Poltergeist", PokemonType.GHOST, MoveCategory.PHYSICAL, 110, 90, 5, "Attacks using target's held item."),

        // Dragon
        PokemonMove("Dragon Claw", PokemonType.DRAGON, MoveCategory.PHYSICAL, 80, 100, 15, "Slashes with huge, sharp dragon claws."),
        PokemonMove("Draco Meteor", PokemonType.DRAGON, MoveCategory.SPECIAL, 130, 90, 5, "Summons meteors; lowers user's SpAtk by 2."),
        PokemonMove("Outrage", PokemonType.DRAGON, MoveCategory.PHYSICAL, 120, 100, 10, "Rampages for 2-3 turns then becomes confused."),

        // Steel
        PokemonMove("Flash Cannon", PokemonType.STEEL, MoveCategory.SPECIAL, 80, 100, 10, "Gathers light and fires; 10% SpD drop chance."),
        PokemonMove("Iron Head", PokemonType.STEEL, MoveCategory.PHYSICAL, 80, 100, 15, "Slams steel head; 30% flinch chance."),

        // Dark
        PokemonMove("Dark Pulse", PokemonType.DARK, MoveCategory.SPECIAL, 80, 100, 15, "Releases horrific aura; 20% flinch chance."),
        PokemonMove("Knock Off", PokemonType.DARK, MoveCategory.PHYSICAL, 65, 100, 20, "Knocks off target's held item with 1.5x damage bonus."),
        PokemonMove("Sucker Punch", PokemonType.DARK, MoveCategory.PHYSICAL, 70, 100, 5, "Strikes first if target is preparing an attack."),

        // Fairy
        PokemonMove("Moonblast", PokemonType.FAIRY, MoveCategory.SPECIAL, 95, 100, 15, "Borrows moon power; 30% SpAtk drop chance."),
        PokemonMove("Play Rough", PokemonType.FAIRY, MoveCategory.PHYSICAL, 90, 90, 10, "Roughhouses with target; 10% Atk drop chance.")
    )

    private val movesByName = allMoves.associateBy { it.name.lowercase() }

    fun findMove(name: String): PokemonMove? = movesByName[name.lowercase()]

    // Canonical / Recommended sets for premier Pokemon
    private val canonicalSets = mapOf(
        149 to listOf("Dragon Claw", "Extreme Speed", "Fire Punch", "Earthquake"), // Dragonite
        249 to listOf("Aeroblast", "Surf", "Psychic", "Recover"), // Lugia
        250 to listOf("Brave Bird", "Flare Blitz", "Earthquake", "Roost"), // Ho-Oh
        445 to listOf("Earthquake", "Dragon Claw", "Stone Edge", "Swords Dance"), // Garchomp
        212 to listOf("U-turn", "Bullet Punch", "Close Combat", "Swords Dance"), // Scizor
        130 to listOf("Waterfall", "Ice Fang", "Earthquake", "Dragon Dance"), // Gyarados
        248 to listOf("Stone Edge", "Crunch", "Earthquake", "Stealth Rock"), // Tyranitar
        94 to listOf("Shadow Ball", "Sludge Bomb", "Focus Blast", "Thunderbolt"), // Gengar
        448 to listOf("Close Combat", "Aura Sphere", "Flash Cannon", "Extreme Speed"), // Lucario
        197 to listOf("Foul Play", "Wish", "Protect", "Toxic"), // Umbreon
        196 to listOf("Psychic", "Shadow Ball", "Dazzling Gleam", "Calm Mind"), // Espeon
        160 to listOf("Waterfall", "Ice Punch", "Crunch", "Dragon Dance"), // Feraligatr
        157 to listOf("Eruption", "Flamethrower", "Focus Blast", "Solar Beam"), // Typhlosion
        154 to listOf("Giga Drain", "Leech Seed", "Reflect", "Light Screen") // Meganium
    )

    // Smart Type-based Generator:
    // Guarantees: STAB 1, STAB 2 (or Ground Earthquake), Elemental Coverage (Ice/Fire/Thunder), Utility (Protect/Recover/Swords Dance)
    fun generateSmartMoveset(pokemon: Pokemon): List<String> {
        val canonical = canonicalSets[pokemon.id]
        if (canonical != null) return canonical

        val result = mutableListOf<String>()

        // 1. Primary STAB
        val primaryMoves = allMoves.filter { it.type == pokemon.primaryType && it.power != null }
        val isPhys = pokemon.attack >= pokemon.spAtk
        val stab1 = primaryMoves.filter { if (isPhys) it.category == MoveCategory.PHYSICAL else it.category == MoveCategory.SPECIAL }
            .maxByOrNull { it.power ?: 0 } ?: primaryMoves.firstOrNull()
        result.add(stab1?.name ?: "Tackle")

        // 2. Secondary STAB or Earthquake
        if (pokemon.secondaryType != null) {
            val secMoves = allMoves.filter { it.type == pokemon.secondaryType && it.power != null }
            val stab2 = secMoves.filter { if (isPhys) it.category == MoveCategory.PHYSICAL else it.category == MoveCategory.SPECIAL }
                .maxByOrNull { it.power ?: 0 } ?: secMoves.firstOrNull()
            result.add(stab2?.name ?: "Earthquake")
        } else {
            result.add(if (isPhys) "Earthquake" else "Earth Power")
        }

        // 3. Elemental Coverage (Ice Beam, Thunderbolt, Flamethrower, Close Combat)
        val coverageMoves = listOf("Ice Beam", "Thunderbolt", "Flamethrower", "Close Combat", "Shadow Ball")
            .filter { name -> result.none { it.equals(name, ignoreCase = true) } }
        result.add(coverageMoves.first())

        // 4. Status/Utility
        result.add(if (pokemon.defense >= 90 || pokemon.spDef >= 90) "Recover" else "Protect")

        return result
    }
}
