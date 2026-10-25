package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabaseHelper
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesManager
import com.example.data.storage.JsonFileStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PokéMaster Hub", appName)
    }

    @Test
    fun testSharedPreferencesPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferencesManager(context)

        prefs.mechanicMode = "gen4"
        assertEquals("gen4", prefs.mechanicMode)
        assertEquals(false, prefs.isModern)

        prefs.quizStreak = 7
        assertEquals(7, prefs.quizStreak)

        prefs.setTypeQuota(PokemonType.FIRE, 5)
        assertEquals(5, prefs.getTypeQuota(PokemonType.FIRE))
    }

    @Test
    fun testSqlitePersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbHelper = AppDatabaseHelper(context)

        // Test rating insertion
        val rating = PokemonRating(
            pokemonId = 25,
            status = RatingStatus.LIKE,
            tags = setOf(RatingTag.ICON, RatingTag.PARTY),
            customTier = Tier.S
        )
        dbHelper.upsertRating(rating)

        val retrievedRatings = dbHelper.getAllRatings()
        assertNotNull(retrievedRatings[25])
        assertEquals(RatingStatus.LIKE, retrievedRatings[25]?.status)
        assertTrue(retrievedRatings[25]!!.tags.contains(RatingTag.ICON))
        assertTrue(retrievedRatings[25]!!.tags.contains(RatingTag.PARTY))
        assertEquals(Tier.S, retrievedRatings[25]?.customTier)

        // Test tracker insertion
        val trackerEntry = PokemonTrackerEntry(
            pokemonId = 152,
            status = TrackerStatus.CAUGHT
        )
        dbHelper.upsertTrackerEntry(trackerEntry)
        val retrievedTracker = dbHelper.getAllTrackerEntries()
        assertEquals(TrackerStatus.CAUGHT, retrievedTracker[152]?.status)
    }

    @Test
    fun testJsonFileStorageAutosave() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = JsonFileStorageManager(context)

        val ratings = mapOf(
            157 to PokemonRating(157, RatingStatus.LIKE, setOf(RatingTag.PARTY), Tier.A)
        )
        val tracker = mapOf(
            157 to PokemonTrackerEntry(157, TrackerStatus.CAUGHT)
        )
        val party = listOf(
            PartySlot(0, 157, "Typhlosion", "Eruption", "Flamethrower", "Focus Blast", "Hidden Power", "Choice Specs", "Blaze")
        )

        val saved = storage.saveToFile(ratings, tracker, party, "modern", 3, 10)
        assertTrue(saved)

        val exportedJson = storage.exportToJson(ratings, tracker, party, "modern")
        assertNotNull(exportedJson)

        val importResult = storage.importFromJson(exportedJson)
        assertNotNull(importResult)
        assertEquals(1, importResult?.ratings?.size)
        assertEquals(157, importResult?.ratings?.first()?.pokemonId)
    }

    @Test
    fun testCanonical1025PokemonAndPreFairySync() {
        val all = com.example.data.repository.PokemonDatabase.allPokemon
        assertEquals(1025, all.size)
        val byId = all.associateBy { it.id }

        // Verify previously miscalculated Pokémon now match official PokéAPI v2 / Game Freak stats & types
        val bulbasaur = byId[1]!!
        assertEquals(PokemonType.GRASS, bulbasaur.primaryType)
        assertEquals(PokemonType.POISON, bulbasaur.secondaryType)
        assertEquals(318, bulbasaur.bst)

        val exploud = byId[295]!!
        assertEquals(PokemonType.NORMAL, exploud.primaryType)
        assertEquals(null, exploud.secondaryType)
        assertEquals(490, exploud.bst)

        val gible = byId[443]!!
        assertEquals(PokemonType.DRAGON, gible.primaryType)
        assertEquals(PokemonType.GROUND, gible.secondaryType)
        assertEquals(300, gible.bst)

        val archen = byId[566]!!
        assertEquals(PokemonType.ROCK, archen.primaryType)
        assertEquals(PokemonType.FLYING, archen.secondaryType)
        assertEquals(401, archen.bst)

        val rowlet = byId[722]!!
        assertEquals(PokemonType.GRASS, rowlet.primaryType)
        assertEquals(PokemonType.FLYING, rowlet.secondaryType)
        assertEquals(320, rowlet.bst)
        assertEquals(1, rowlet.hisuiDexNo)

        val dartrix = byId[723]!!
        assertEquals(PokemonType.GRASS, dartrix.primaryType)
        assertEquals(PokemonType.FLYING, dartrix.secondaryType)
        assertEquals(420, dartrix.bst)
        assertEquals(2, dartrix.hisuiDexNo)

        val tsareena = byId[763]!!
        assertEquals(PokemonType.GRASS, tsareena.primaryType)
        assertEquals(null, tsareena.secondaryType)
        assertEquals(510, tsareena.bst)

        val drakloak = byId[886]!!
        assertEquals(PokemonType.DRAGON, drakloak.primaryType)
        assertEquals(PokemonType.GHOST, drakloak.secondaryType)
        assertEquals(410, drakloak.bst)

        // Verify Pre-Fairy Sync for Clefable (#36), Togekiss (#468), Gardevoir (#282), and Magnemite (#81)
        val clefable = byId[36]!!
        assertEquals(Pair(PokemonType.NORMAL, null), clefable.effectiveTypes(TypeEra.GEN2_5_17T))
        assertEquals(Pair(PokemonType.FAIRY, null), clefable.effectiveTypes(TypeEra.GEN6_9_18T))

        val togekiss = byId[468]!!
        assertEquals(Pair(PokemonType.NORMAL, PokemonType.FLYING), togekiss.effectiveTypes(TypeEra.GEN2_5_17T))
        assertEquals(Pair(PokemonType.FAIRY, PokemonType.FLYING), togekiss.effectiveTypes(TypeEra.GEN6_9_18T))

        val magnemite = byId[81]!!
        assertEquals(Pair(PokemonType.ELECTRIC, null), magnemite.effectiveTypes(TypeEra.GEN1_15T))
        assertEquals(Pair(PokemonType.ELECTRIC, PokemonType.STEEL), magnemite.effectiveTypes(TypeEra.GEN2_5_17T))
    }
}
