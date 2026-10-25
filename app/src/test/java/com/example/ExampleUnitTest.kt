package com.example

import com.example.data.model.PokemonType
import com.example.data.model.RatingStatus
import com.example.data.model.RatingTag
import com.example.data.model.Tier
import com.example.data.repository.PokemonDatabase
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNationalDatabaseCoverage() {
        val allMons = PokemonDatabase.allPokemon
        assertEquals(1025, allMons.size)

        // Bulbasaur #1 to Pecharunt #1025
        assertEquals("Bulbasaur", allMons[0].name)
        assertEquals("Pecharunt", allMons[1024].name)

        // Verify Johto entries exist
        val johtoMons = PokemonDatabase.johtoDexPokemon
        assertTrue(johtoMons.isNotEmpty())
        assertTrue(johtoMons.any { it.name == "Meganium" })
    }

    @Test
    fun testTypeEffectivenessGen4VsModern() {
        // Water/Ground (e.g., Quagsire/Swampert) has a 4x double weakness to Grass
        val grassVsWaterGround = PokemonType.getDefendingMultiplier(
            attacking = PokemonType.GRASS,
            defendingPrimary = PokemonType.WATER,
            defendingSecondary = PokemonType.GROUND,
            isModern = true
        )
        assertEquals(4.0f, grassVsWaterGround, 0.001f)

        // Gen 4 quirk: Steel resisted Dark and Ghost in Gen 4 (0.5x)
        val darkVsSteelGen4 = PokemonType.getMultiplier(PokemonType.DARK, PokemonType.STEEL, isModern = false)
        assertEquals(0.5f, darkVsSteelGen4, 0.001f)

        // Modern: Steel takes neutral damage from Dark (1.0x)
        val darkVsSteelModern = PokemonType.getMultiplier(PokemonType.DARK, PokemonType.STEEL, isModern = true)
        assertEquals(1.0f, darkVsSteelModern, 0.001f)

        // Fairy does not exist in Gen 4
        val dragonVsFairyGen4 = PokemonType.getMultiplier(PokemonType.DRAGON, PokemonType.FAIRY, isModern = false)
        assertEquals(1.0f, dragonVsFairyGen4, 0.001f)

        // Fairy is immune to Dragon in Modern (0x)
        val dragonVsFairyModern = PokemonType.getMultiplier(PokemonType.DRAGON, PokemonType.FAIRY, isModern = true)
        assertEquals(0.0f, dragonVsFairyModern, 0.001f)
    }

    @Test
    fun testTierClassification() {
        // BST >= 580 -> S-Tier
        assertEquals(Tier.S, Tier.fromBst(600, isTrap = false))
        assertEquals(Tier.A, Tier.fromBst(530, isTrap = false))
        assertEquals(Tier.B, Tier.fromBst(490, isTrap = false))
        assertEquals(Tier.C, Tier.fromBst(420, isTrap = false))
        assertEquals(Tier.D, Tier.fromBst(350, isTrap = false))
        assertEquals(Tier.F, Tier.fromBst(200, isTrap = false))

        // Noob Trap override
        assertEquals(Tier.TRAP, Tier.fromBst(670, isTrap = true))
    }

    @Test
    fun testMovesetGeneration() {
        val dragonite = PokemonDatabase.allPokemon.first { it.id == 149 || it.name == "Dragonite" }
        val moves = com.example.data.model.MovesDatabase.generateSmartMoveset(dragonite)
        assertEquals(4, moves.size)
        assertTrue(moves.contains("Dragon Claw"))
        assertTrue(moves.contains("Extreme Speed"))
    }

    @Test
    fun testMovesDatabaseLookup() {
        val eq = com.example.data.model.MovesDatabase.findMove("Earthquake")
        assertNotNull(eq)
        assertEquals(PokemonType.GROUND, eq?.type)
        assertEquals(100, eq?.power)
    }
}

