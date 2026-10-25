package com.example

import com.example.data.BattleDrillsEngine
import com.example.data.DrillCategoryFilter
import com.example.data.DrillQuestionCategory
import com.example.data.model.PokemonType
import com.example.data.repository.PokemonDatabase
import com.example.data.repository.PokemonNamesData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleDrillsEngineTest {

    @Test
    fun verify171TypeCombinationsAnd9UnusedCombos() {
        val allCombos = BattleDrillsEngine.ALL_171_COMBOS
        assertEquals(171, allCombos.size)

        val singleTypes = allCombos.filter { it.secondary == null }
        val dualTypes = allCombos.filter { it.secondary != null }
        assertEquals(18, singleTypes.size)
        assertEquals(153, dualTypes.size)

        val unusedCombos = allCombos.filter { it.isUnusedInMainSeries }
        assertEquals(9, unusedCombos.size)

        val expectedUnusedNames = setOf(
            "Normal/Ice", "Normal/Bug", "Normal/Rock", "Normal/Steel",
            "Fire/Fairy", "Ice/Poison", "Ground/Fairy",
            "Bug/Dragon", "Rock/Ghost"
        )
        val actualUnusedNames = unusedCombos.map { it.displayName }.toSet()
        assertEquals(expectedUnusedNames, actualUnusedNames)
    }

    @Test
    fun verifyAll3591QuestionsGenerateValidly() {
        assertEquals(3591, BattleDrillsEngine.getIndicesForFilter(DrillCategoryFilter.ALL).count())
        assertEquals(3078, BattleDrillsEngine.getIndicesForFilter(DrillCategoryFilter.ADVANTAGE).count())
        assertEquals(171, BattleDrillsEngine.getIndicesForFilter(DrillCategoryFilter.STAB).count())
        assertEquals(342, BattleDrillsEngine.getIndicesForFilter(DrillCategoryFilter.RESISTANCE).count())

        var advantageCount = 0
        var stabCount = 0
        var resistCount = 0
        var immuneCount = 0

        for (i in 0 until 3591) {
            val q = BattleDrillsEngine.generateQuestionByGlobalIndex(i, isModern = true)
            assertEquals(i + 1, q.globalNumber)
            assertTrue("Question $i has invalid correctIndex ${q.correctIndex}", q.correctIndex in q.options.indices)
            when (q.category) {
                DrillQuestionCategory.ADVANTAGE -> {
                    advantageCount++
                    assertEquals(3, q.options.size)
                }
                DrillQuestionCategory.STAB -> stabCount++
                DrillQuestionCategory.RESISTANCE_RESIST -> resistCount++
                DrillQuestionCategory.RESISTANCE_IMMUNE -> immuneCount++
            }
        }

        assertEquals(3078, advantageCount)
        assertEquals(171, stabCount)
        assertEquals(171, resistCount)
        assertEquals(171, immuneCount)
    }

    @Test
    fun verifyGen4ExcludesFairyQuestionsAndTypes() {
        val gen4Indices = BattleDrillsEngine.getValidGlobalIndices(DrillCategoryFilter.ALL, isModern = false)
        assertTrue(gen4Indices.isNotEmpty())
        assertTrue(gen4Indices.size < 3591)

        for (idx in gen4Indices) {
            assertFalse("Gen 4 question index $idx should not involve Fairy", BattleDrillsEngine.involvesFairy(idx))
            val q = BattleDrillsEngine.generateQuestionByGlobalIndex(idx, isModern = false)
            assertFalse("Gen 4 prompt should not mention Fairy: ${q.prompt}", q.prompt.contains("Fairy"))
            q.optionTypes.forEach { typesList ->
                assertFalse("Gen 4 option should not include Fairy", typesList.contains(PokemonType.FAIRY))
            }
        }
    }

    @Test
    fun verifyGen4VsModernChartDifferences() {
        // In Gen 4, Steel resisted Ghost and Dark (0.5x); in Modern, it takes 1.0x
        assertEquals(0.5f, BattleDrillsEngine.calculateEffectiveness(PokemonType.GHOST, PokemonType.STEEL, null, isModern = false))
        assertEquals(1.0f, BattleDrillsEngine.calculateEffectiveness(PokemonType.GHOST, PokemonType.STEEL, null, isModern = true))
    }

    @Test
    fun verifyRegionalDexCountsAndDatabaseIntegrity() {
        assertEquals(256, PokemonNamesData.HGSS_JOHTO_256_IDS.size)
        assertEquals(457, PokemonNamesData.KALOS_457_IDS.size)
        assertEquals(242, PokemonNamesData.HISUI_242_IDS.size)

        val allMon = PokemonDatabase.allPokemon
        assertEquals(1025, allMon.size)
        assertEquals(256, allMon.count { it.johtoDexNo != null })
        assertEquals(457, allMon.count { it.kalosDexNo != null })
        assertEquals(242, allMon.count { it.hisuiDexNo != null })
    }

    @Test
    fun verifyPreFairySyncLogic() {
        val pokeMap = PokemonDatabase.allPokemon.associateBy { it.id }

        // Test 6 Pure Fairy -> Pure Normal in Gen 1 & Gen 2-5
        val pureFairyIds = listOf(35, 36, 173, 175, 209, 210) // Clefairy, Clefable, Cleffa, Togepi, Snubbull, Granbull
        for (id in pureFairyIds) {
            val mon = pokeMap[id] ?: continue
            val (gen2Prim, gen2Sec) = mon.effectiveTypes(com.example.data.model.TypeEra.GEN_2_5_17)
            assertEquals("Pokemon $id should be NORMAL in Gen 2-5", PokemonType.NORMAL, gen2Prim)
            assertEquals("Pokemon $id should have no secondary in Gen 2-5", null, gen2Sec)
        }

        // Test Dual-types: Marill (183), Azumarill (184) -> Water
        val marill = pokeMap[183]!!
        val (marillPrim, marillSec) = marill.effectiveTypes(com.example.data.model.TypeEra.GEN_2_5_17)
        assertEquals(PokemonType.WATER, marillPrim)
        assertEquals(null, marillSec)

        // Togetic (176) & Togekiss (468) -> Normal / Flying
        val togekiss = pokeMap[468]!!
        val (togekissPrim, togekissSec) = togekiss.effectiveTypes(com.example.data.model.TypeEra.GEN_2_5_17)
        assertEquals(PokemonType.NORMAL, togekissPrim)
        assertEquals(PokemonType.FLYING, togekissSec)

        // Gardevoir (282) -> Psychic
        val gardevoir = pokeMap[282]!!
        val (gardPrim, gardSec) = gardevoir.effectiveTypes(com.example.data.model.TypeEra.GEN_2_5_17)
        assertEquals(PokemonType.PSYCHIC, gardPrim)
        assertEquals(null, gardSec)

        // Gen 1: Magnemite (81) & Magneton (82) -> Pure Electric
        val magnemite = pokeMap[81]!!
        val (magPrim, magSec) = magnemite.effectiveTypes(com.example.data.model.TypeEra.GEN_1_15)
        assertEquals(PokemonType.ELECTRIC, magPrim)
        assertEquals(null, magSec)
    }
}


