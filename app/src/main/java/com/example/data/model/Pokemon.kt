package com.example.data.model

data class Pokemon(
    val id: Int,
    val name: String,
    val primaryType: PokemonType,
    val secondaryType: PokemonType? = null,
    val hp: Int = 50,
    val attack: Int = 50,
    val defense: Int = 50,
    val spAtk: Int = 50,
    val spDef: Int = 50,
    val speed: Int = 50,
    val generation: Int = 1,
    val johtoDexNo: Int? = null,
    val kalosDexNo: Int? = null,
    val hisuiDexNo: Int? = null,
    val familyId: String = "fam_$id",
    val familyName: String = name,
    val evolutionStage: Int = 1,
    val isSingular: Boolean = false,
    val isTrap: Boolean = false,
    val trapReason: String? = null,
    val isLegendary: Boolean = false
) {
    val bst: Int
        get() = hp + attack + defense + spAtk + spDef + speed

    val defaultTier: Tier
        get() = Tier.fromBst(bst, isTrap)

    fun formattedId(): String = String.format("#%04d", id)

    fun formattedShortId(): String = String.format("#%03d", id)

    fun formattedJohtoId(): String? = johtoDexNo?.let { String.format("#%03d", it) }

    fun spriteUrl(isShiny: Boolean = false): String {
        return if (isShiny) {
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$id.png"
        } else {
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
        }
    }

    fun officialArtworkUrl(): String {
        return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    }

    fun cryUrl(): String {
        return "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest/$id.ogg"
    }

    fun effectiveTypes(era: TypeEra): Pair<PokemonType, PokemonType?> {
        return PreFairySync.getEraTypes(id, primaryType, secondaryType, era)
    }
}

enum class TypeEra(val label: String, val shortLabel: String) {
    ALL_TYPES("All Types", "All"),
    GEN_1_15("Gen I (15 Types)", "Gen 1 (15T)"),
    GEN_2_5_17("Gen II - V (17 Types)", "Gen 2-5 (17T)"),
    GEN_6_9_18("Gen VI - IX (18 Types)", "Gen 6-9 (18T)")
}

object PreFairySync {
    /**
     * Automatic synchronization for the 22 pre-Fairy Pokémon + Magnemite/Magneton:
     * - Gen I (15 Types): No Dark, Steel, or Fairy. Magnemite & Magneton become pure Electric. Pre-Fairy 22 become old types.
     * - Gen II-V (17 Types): No Fairy. Steel & Dark exist. Pre-Fairy 22 become old types.
     * - Gen VI-IX (18 Types) / All Types: Modern typing with Fairy.
     */
    fun getEraTypes(
        id: Int,
        modernPrimary: PokemonType,
        modernSecondary: PokemonType?,
        era: TypeEra
    ): Pair<PokemonType, PokemonType?> {
        return when (era) {
            TypeEra.GEN_1_15 -> {
                // Gen I: Magnemite & Magneton were pure Electric
                if (id == 81 || id == 82) {
                    PokemonType.ELECTRIC to null
                } else {
                    getPreFairyPair(id) ?: run {
                        val prim = if (modernPrimary == PokemonType.FAIRY || modernPrimary == PokemonType.STEEL || modernPrimary == PokemonType.DARK) PokemonType.NORMAL else modernPrimary
                        val sec = if (modernSecondary == PokemonType.FAIRY || modernSecondary == PokemonType.STEEL || modernSecondary == PokemonType.DARK) null else modernSecondary
                        prim to sec
                    }
                }
            }
            TypeEra.GEN_2_5_17 -> {
                getPreFairyPair(id) ?: run {
                    val prim = if (modernPrimary == PokemonType.FAIRY) PokemonType.NORMAL else modernPrimary
                    val sec = if (modernSecondary == PokemonType.FAIRY) null else modernSecondary
                    prim to sec
                }
            }
            TypeEra.ALL_TYPES, TypeEra.GEN_6_9_18 -> {
                modernPrimary to modernSecondary
            }
        }
    }

    private fun getPreFairyPair(id: Int): Pair<PokemonType, PokemonType?>? {
        return when (id) {
            // 6 Pure Fairy -> Pure Normal
            35, 36, 173, 175, 209, 210 -> PokemonType.NORMAL to null
            // 16 Dual-Type Fairy -> Old types
            39, 40, 174, 298 -> PokemonType.NORMAL to null
            183, 184 -> PokemonType.WATER to null
            176, 468 -> PokemonType.NORMAL to PokemonType.FLYING
            122, 280, 281, 282, 439 -> PokemonType.PSYCHIC to null
            303 -> PokemonType.STEEL to null
            546, 547 -> PokemonType.GRASS to null
            else -> null
        }
    }
}

data class PokemonRating(
    val pokemonId: Int,
    val status: RatingStatus = RatingStatus.UNRATED,
    val tags: Set<RatingTag> = emptySet(),
    val customTier: Tier? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class PokemonTrackerEntry(
    val pokemonId: Int,
    val status: TrackerStatus = TrackerStatus.UNSEEN,
    val updatedAt: Long = System.currentTimeMillis()
)

data class RecentRatingAction(
    val id: Long = 0,
    val pokemonId: Int,
    val pokemonName: String,
    val actionType: String,
    val previousStatus: RatingStatus,
    val previousTags: Set<RatingTag>,
    val previousTier: Tier?,
    val timestamp: Long = System.currentTimeMillis()
)

data class RecentTrackerAction(
    val id: Long = 0,
    val pokemonId: Int,
    val pokemonName: String,
    val previousStatus: TrackerStatus,
    val newStatus: TrackerStatus,
    val timestamp: Long = System.currentTimeMillis()
)

data class PartySlot(
    val slotIndex: Int, // 0..5
    val pokemonId: Int? = null,
    val nickname: String? = null,
    val move1: String = "Tackle",
    val move2: String = "Quick Attack",
    val move3: String = "Protect",
    val move4: String = "Rest",
    val item: String = "Leftovers",
    val ability: String = "Standard",
    val move1Type: PokemonType = PokemonType.NORMAL,
    val move1Category: String = "Physical",
    val move2Type: PokemonType = PokemonType.WATER,
    val move2Category: String = "Special",
    val move3Type: PokemonType = PokemonType.ICE,
    val move3Category: String = "Special",
    val move4Type: PokemonType = PokemonType.GROUND,
    val move4Category: String = "Physical",
    val secretType: PokemonType? = null,
    val secretCategory: String = "Special",
    val isShiny: Boolean = false
)
