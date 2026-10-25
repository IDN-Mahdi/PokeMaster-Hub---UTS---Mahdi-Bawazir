package com.example.data.repository

import com.example.data.model.Pokemon
import com.example.data.model.PokemonType
import com.example.data.model.Tier

object PokemonDatabase {

    private val johtoIndexMap: Map<Int, Int> by lazy {
        PokemonNamesData.HGSS_JOHTO_256_IDS.mapIndexed { idx, natId -> natId to (idx + 1) }.toMap()
    }

    private val kalosIndexMap: Map<Int, Int> by lazy {
        PokemonNamesData.KALOS_457_IDS.mapIndexed { idx, natId -> natId to (idx + 1) }.toMap()
    }

    private val hisuiIndexMap: Map<Int, Int> by lazy {
        PokemonNamesData.HISUI_242_IDS.mapIndexed { idx, natId -> natId to (idx + 1) }.toMap()
    }

    private val familyMap: Map<Int, String> by lazy {
        val map = HashMap<Int, String>()
        PokemonNamesData.CANONICAL_FAMILIES.forEachIndexed { idx, ids ->
            val famId = "fam_${idx + 1}"
            ids.forEach { id -> map[id] = famId }
        }
        map
    }

    private val familySizeMap: Map<String, Int> by lazy {
        val map = HashMap<String, Int>()
        PokemonNamesData.CANONICAL_FAMILIES.forEachIndexed { idx, ids ->
            val famId = "fam_${idx + 1}"
            map[famId] = ids.size
        }
        map
    }

    // Curated Competitive Noob Traps with tactical explanations
    private val NOOB_TRAP_REASONS: Map<Int, String> = mapOf(
        6 to "Charizard: 4x Stealth Rock weakness removes 50% HP on switch-in without Heavy-Duty Boots.",
        12 to "Butterfree: 4x Rock weakness and low 395 BST leave it frail against faster attackers.",
        15 to "Beedrill: 395 BST and fragile defenses make it easily revenge-killed.",
        20 to "Raticate: Requires Guts status activation to threaten walls; outsped by common sweepers.",
        24 to "Arbok: Intimidate utility is offset by 448 BST and lack of reliable recovery.",
        47 to "Parasect: 4x Fire and 4x Flying weaknesses with 30 Base Speed make it highly vulnerable.",
        83 to "Farfetch'd: 377 BST is too low for standard play without extreme Stick crit luck.",
        95 to "Onix: 45 Base Attack (lower than Oddish!) makes it passive until evolved into Steelix.",
        132 to "Ditto: Without Imposter hidden ability, transforming wastes a turn while taking a hit.",
        136 to "Flareon: 130 Base Attack is hindered by 65 Speed, 65 HP, and Stealth Rock chip.",
        154 to "Meganium: Pure Grass defensive typing suffers from 5 common weaknesses and passive offenses.",
        162 to "Furret: 415 BST lacks the bulk or offensive power to break Steel/Rock walls.",
        166 to "Ledian: 35 Base Attack and 4x Rock weakness prevent it from dealing meaningful damage.",
        168 to "Ariados: 40 Base Speed and frail bulk leave it outclassed as a sticky web/hazard lead.",
        176 to "Togetic: Requires evolution into Togekiss (Gen 4) or Eviolite support to function.",
        192 to "Sunflora: 30 Base Speed and pure Grass typing leave it exposed even under Sun.",
        201 to "Unown: Only learns a single move (Hidden Power) with a 336 Base Stat Total.",
        202 to "Wobbuffet: Zero direct attacks; completely shut down by Taunt, Toxic, or U-turn.",
        206 to "Dunsparce: Relies entirely on Serene Grace ParaFlinch RNG with 45 Base Speed.",
        219 to "Magcargo: Dual 4x weaknesses to Water and Ground with 38 Base Speed.",
        222 to "Corsola: 410 BST and Rock/Water typing leave it vulnerable to Grass, Electric, and Ground.",
        225 to "Delibird: 330 BST and 4x Stealth Rock weakness make it one of the frailest Ice/Flying types.",
        235 to "Smeargle: 250 BST means it dies to almost any priority move if Focus Sash is broken.",
        289 to "Slaking: Truant ability forces it to loaf around every second turn, giving opponents free setup.",
        292 to "Shedinja: 1 HP Wonder Guard gimmick is instantly KO'd by Stealth Rock, Sandstorm, Toxic, or Will-O-Wisp.",
        302 to "Sableye: 380 BST without Prankster leaves it easily overwhelmed by neutral hits.",
        327 to "Spinda: 360 BST (60 across the board) is unusable without Contrary Superpower support.",
        351 to "Castform: 420 BST and reliance on manual weather turns make it too slow to set up.",
        370 to "Luvdisc: 330 BST offers almost no offensive or defensive presence.",
        402 to "Kricketune: 384 BST falls off rapidly after early-game badge battles.",
        416 to "Vespiquen: 4x Rock weakness and 40 Base Speed undermine its defensive stats.",
        479 to "Rotom (Base): Base Ghost/Electric form has lower stats (440 BST) than Appliance forms (520 BST).",
        486 to "Regigigas: Slow Start halves Attack and Speed for 5 full turns upon entering battle.",
        567 to "Archeops: Defeatist ability halves Attack and Sp.Atk whenever HP drops to 50% or below."
    )

    // Competitive S-Tier & A-Tier overrides for lower-BST powerhouses
    private val S_TIER_OVERRIDES: Set<Int> = setOf(
        94, 130, 143, 149, 150, 151, 212, 227, 242, 248, 249, 250, 254, 257, 260,
        373, 376, 380, 381, 382, 383, 384, 385, 386, 392, 445, 448, 461, 462, 472,
        483, 484, 485, 487, 491, 493, 530, 598, 635, 637, 645, 658, 681, 778, 785,
        812, 815, 823, 887, 888, 889, 890, 892, 898, 901, 908, 911, 914, 977, 979,
        980, 983, 984, 987, 990, 991, 992, 998, 1000, 1002, 1003, 1004, 1005, 1006,
        1007, 1008, 1009, 1017, 1018, 1020, 1021, 1024
    )

    private val A_TIER_OVERRIDES: Set<Int> = setOf(
        3, 6, 9, 34, 59, 65, 68, 80, 112, 121, 131, 134, 135, 142, 145, 157, 160,
        169, 181, 184, 196, 197, 199, 205, 208, 214, 229, 230, 232, 233, 243, 244,
        245, 251, 272, 282, 286, 306, 330, 342, 350, 356, 365, 389, 395, 398, 407,
        423, 450, 452, 454, 464, 465, 466, 467, 468, 469, 473, 474, 475, 477, 478
    )

    val allPokemon: List<Pokemon> by lazy {
        val names = PokemonNamesData.NAMES_1025
        PokemonCanonicalData.RAW_CANONICAL_1025.map { line ->
            // Format: id:PRIM:SEC:hp,atk,def,spa,spd,spe:isLeg
            val parts = line.split(":")
            val id = parts[0].toInt()
            val primary = PokemonType.valueOf(parts[1])
            val secondary = if (parts[2].isEmpty()) null else PokemonType.valueOf(parts[2])
            val st = parts[3].split(",").map { it.toInt() }
            val hp = st[0]
            val atk = st[1]
            val def = st[2]
            val spa = st[3]
            val spd = st[4]
            val spe = st[5]
            val bst = hp + atk + def + spa + spd + spe
            val isLeg = parts[4] == "1"

            val famId = familyMap[id] ?: "fam_single_$id"
            val isSingle = (familySizeMap[famId] ?: 1) == 1
            val trapReason = NOOB_TRAP_REASONS[id]
            val isTrap = trapReason != null

            val tier = when {
                isTrap -> Tier.TRAP
                S_TIER_OVERRIDES.contains(id) || bst >= 600 -> Tier.S
                A_TIER_OVERRIDES.contains(id) || bst >= 525 -> Tier.A
                bst >= 470 -> Tier.B
                bst >= 400 -> Tier.C
                bst >= 310 -> Tier.D
                else -> Tier.F
            }

            Pokemon(
                id = id,
                name = names.getOrElse(id - 1) { "Pokemon #$id" },
                primaryType = primary,
                secondaryType = secondary,
                hp = hp,
                attack = atk,
                defense = def,
                spAtk = spa,
                spDef = spd,
                speed = spe,
                johtoDexNo = johtoIndexMap[id],
                kalosDexNo = kalosIndexMap[id],
                hisuiDexNo = hisuiIndexMap[id],
                generation = generationForId(id),
                isTrap = isTrap,
                trapReason = trapReason,
                familyId = famId,
                isLegendary = isLeg,
                isSingular = isSingle
            )
        }
    }

    val johtoDexPokemon: List<Pokemon> by lazy {
        val byId = allPokemon.associateBy { it.id }
        PokemonNamesData.HGSS_JOHTO_256_IDS.mapNotNull { byId[it] }
    }

    val kalosDexPokemon: List<Pokemon> by lazy {
        val byId = allPokemon.associateBy { it.id }
        PokemonNamesData.KALOS_457_IDS.mapNotNull { byId[it] }
    }

    val hisuiDexPokemon: List<Pokemon> by lazy {
        val byId = allPokemon.associateBy { it.id }
        PokemonNamesData.HISUI_242_IDS.mapNotNull { byId[it] }
    }

    val johtoEvolutionFamilies: List<List<Pokemon>> by lazy {
        val byId = allPokemon.associateBy { it.id }
        PokemonNamesData.JOHTO_FAMILIES.mapNotNull { ids ->
            val members = ids.mapNotNull { byId[it] }
            if (members.isNotEmpty()) members else null
        }
    }

    val johtoFamiliesList: List<List<Pokemon>>
        get() = johtoEvolutionFamilies

    val canonicalEvolutionFamilies: List<List<Pokemon>> by lazy {
        val byId = allPokemon.associateBy { it.id }
        PokemonNamesData.CANONICAL_FAMILIES.mapNotNull { ids ->
            val members = ids.mapNotNull { byId[it] }
            if (members.isNotEmpty()) members else null
        }
    }

    val familyMembersByFamilyId: Map<String, List<Pokemon>> by lazy {
        allPokemon.groupBy { it.familyId }.mapValues { (_, list) ->
            list.sortedBy { it.bst }
        }
    }

    private fun generationForId(id: Int): Int = when (id) {
        in 1..151 -> 1
        in 152..251 -> 2
        in 252..386 -> 3
        in 387..493 -> 4
        in 494..649 -> 5
        in 650..721 -> 6
        in 722..809 -> 7
        in 810..905 -> 8
        else -> 9
    }
}
