package com.example.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.*

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "pokemaster_hub.db"
        const val DATABASE_VERSION = 2

        const val TABLE_RATINGS = "pokemon_ratings"
        const val COL_R_POKEMON_ID = "pokemon_id"
        const val COL_R_STATUS = "status"
        const val COL_R_TAGS = "tags"
        const val COL_R_CUSTOM_TIER = "custom_tier"
        const val COL_R_UPDATED_AT = "updated_at"

        const val TABLE_TRACKER = "pokedex_tracker"
        const val COL_T_POKEMON_ID = "pokemon_id"
        const val COL_T_STATUS = "status"
        const val COL_T_UPDATED_AT = "updated_at"

        const val TABLE_RECENT = "recent_ratings"
        const val COL_REC_ID = "id"
        const val COL_REC_POKEMON_ID = "pokemon_id"
        const val COL_REC_POKEMON_NAME = "pokemon_name"
        const val COL_REC_ACTION = "action_type"
        const val COL_REC_PREV_STATUS = "prev_status"
        const val COL_REC_PREV_TAGS = "prev_tags"
        const val COL_REC_PREV_TIER = "prev_tier"
        const val COL_REC_TIMESTAMP = "timestamp"

        const val TABLE_PARTY = "party_roster"
        const val COL_P_SLOT = "slot_index"
        const val COL_P_POKEMON_ID = "pokemon_id"
        const val COL_P_NICKNAME = "nickname"
        const val COL_P_MOVE1 = "move_1"
        const val COL_P_MOVE2 = "move_2"
        const val COL_P_MOVE3 = "move_3"
        const val COL_P_MOVE4 = "move_4"
        const val COL_P_ITEM = "item"
        const val COL_P_ABILITY = "ability"
        const val COL_P_MOVE_META = "move_meta"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_RATINGS (
                $COL_R_POKEMON_ID INTEGER PRIMARY KEY,
                $COL_R_STATUS TEXT NOT NULL,
                $COL_R_TAGS TEXT NOT NULL,
                $COL_R_CUSTOM_TIER TEXT,
                $COL_R_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TRACKER (
                $COL_T_POKEMON_ID INTEGER PRIMARY KEY,
                $COL_T_STATUS TEXT NOT NULL,
                $COL_T_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_RECENT (
                $COL_REC_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REC_POKEMON_ID INTEGER NOT NULL,
                $COL_REC_POKEMON_NAME TEXT NOT NULL,
                $COL_REC_ACTION TEXT NOT NULL,
                $COL_REC_PREV_STATUS TEXT NOT NULL,
                $COL_REC_PREV_TAGS TEXT NOT NULL,
                $COL_REC_PREV_TIER TEXT,
                $COL_REC_TIMESTAMP INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PARTY (
                $COL_P_SLOT INTEGER PRIMARY KEY,
                $COL_P_POKEMON_ID INTEGER,
                $COL_P_NICKNAME TEXT,
                $COL_P_MOVE1 TEXT,
                $COL_P_MOVE2 TEXT,
                $COL_P_MOVE3 TEXT,
                $COL_P_MOVE4 TEXT,
                $COL_P_ITEM TEXT,
                $COL_P_ABILITY TEXT,
                $COL_P_MOVE_META TEXT
            )
            """.trimIndent()
        )

        for (i in 0..5) {
            val cv = ContentValues().apply {
                put(COL_P_SLOT, i)
                putNull(COL_P_POKEMON_ID)
                putNull(COL_P_NICKNAME)
                put(COL_P_MOVE1, "Tackle")
                put(COL_P_MOVE2, "Quick Attack")
                put(COL_P_MOVE3, "Protect")
                put(COL_P_MOVE4, "Rest")
                put(COL_P_ITEM, "Leftovers")
                put(COL_P_ABILITY, "Standard")
                put(COL_P_MOVE_META, "NORMAL:Physical|WATER:Special|ICE:Special|GROUND:Physical|NONE:Special")
            }
            db.insert(TABLE_PARTY, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PARTY ADD COLUMN $COL_P_MOVE_META TEXT")
            } catch (_: Exception) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_PARTY")
                onCreate(db)
            }
        }
    }

    // --- RATINGS CRUD ---
    fun getAllRatings(): Map<Int, PokemonRating> {
        val map = HashMap<Int, PokemonRating>()
        val db = readableDatabase
        val cursor = db.query(TABLE_RATINGS, null, null, null, null, null, null)
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_R_POKEMON_ID)
            val statusIdx = it.getColumnIndexOrThrow(COL_R_STATUS)
            val tagsIdx = it.getColumnIndexOrThrow(COL_R_TAGS)
            val tierIdx = it.getColumnIndexOrThrow(COL_R_CUSTOM_TIER)
            val updatedIdx = it.getColumnIndexOrThrow(COL_R_UPDATED_AT)

            while (it.moveToNext()) {
                val pokemonId = it.getInt(idIdx)
                val statusStr = it.getString(statusIdx)
                val tagsStr = it.getString(tagsIdx)
                val tierStr = it.getString(tierIdx)
                val updated = it.getLong(updatedIdx)

                val status = try { RatingStatus.valueOf(statusStr) } catch (_: Exception) { RatingStatus.UNRATED }
                val tags = parseTags(tagsStr)
                val customTier = Tier.fromString(tierStr)

                map[pokemonId] = PokemonRating(pokemonId, status, tags, customTier, updated)
            }
        }
        return map
    }

    fun upsertRating(rating: PokemonRating) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_R_POKEMON_ID, rating.pokemonId)
            put(COL_R_STATUS, rating.status.name)
            put(COL_R_TAGS, serializeTags(rating.tags))
            put(COL_R_CUSTOM_TIER, rating.customTier?.name)
            put(COL_R_UPDATED_AT, rating.updatedAt)
        }
        db.insertWithOnConflict(TABLE_RATINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- TRACKER CRUD ---
    fun getAllTrackerEntries(): Map<Int, PokemonTrackerEntry> {
        val map = HashMap<Int, PokemonTrackerEntry>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TRACKER, null, null, null, null, null, null)
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_T_POKEMON_ID)
            val statusIdx = it.getColumnIndexOrThrow(COL_T_STATUS)
            val updatedIdx = it.getColumnIndexOrThrow(COL_T_UPDATED_AT)

            while (it.moveToNext()) {
                val pokemonId = it.getInt(idIdx)
                val statusStr = it.getString(statusIdx)
                val updated = it.getLong(updatedIdx)

                val status = try { TrackerStatus.valueOf(statusStr) } catch (_: Exception) { TrackerStatus.UNSEEN }
                map[pokemonId] = PokemonTrackerEntry(pokemonId, status, updated)
            }
        }
        return map
    }

    fun upsertTrackerEntry(entry: PokemonTrackerEntry) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_T_POKEMON_ID, entry.pokemonId)
            put(COL_T_STATUS, entry.status.name)
            put(COL_T_UPDATED_AT, entry.updatedAt)
        }
        db.insertWithOnConflict(TABLE_TRACKER, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- RECENT 10 RATINGS CRUD ---
    fun getRecentRatings(limit: Int = 10): List<RecentRatingAction> {
        val list = mutableListOf<RecentRatingAction>()
        val db = readableDatabase
        val cursor = db.query(TABLE_RECENT, null, null, null, null, null, "$COL_REC_ID DESC", limit.toString())
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(COL_REC_ID)
            val pIdIdx = it.getColumnIndexOrThrow(COL_REC_POKEMON_ID)
            val nameIdx = it.getColumnIndexOrThrow(COL_REC_POKEMON_NAME)
            val actionIdx = it.getColumnIndexOrThrow(COL_REC_ACTION)
            val prevStatIdx = it.getColumnIndexOrThrow(COL_REC_PREV_STATUS)
            val prevTagsIdx = it.getColumnIndexOrThrow(COL_REC_PREV_TAGS)
            val prevTierIdx = it.getColumnIndexOrThrow(COL_REC_PREV_TIER)
            val tsIdx = it.getColumnIndexOrThrow(COL_REC_TIMESTAMP)

            while (it.moveToNext()) {
                list.add(
                    RecentRatingAction(
                        id = it.getLong(idIdx),
                        pokemonId = it.getInt(pIdIdx),
                        pokemonName = it.getString(nameIdx),
                        actionType = it.getString(actionIdx),
                        previousStatus = try { RatingStatus.valueOf(it.getString(prevStatIdx)) } catch (_: Exception) { RatingStatus.UNRATED },
                        previousTags = parseTags(it.getString(prevTagsIdx)),
                        previousTier = Tier.fromString(it.getString(prevTierIdx)),
                        timestamp = it.getLong(tsIdx)
                    )
                )
            }
        }
        return list
    }

    fun addRecentRating(action: RecentRatingAction) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_REC_POKEMON_ID, action.pokemonId)
            put(COL_REC_POKEMON_NAME, action.pokemonName)
            put(COL_REC_ACTION, action.actionType)
            put(COL_REC_PREV_STATUS, action.previousStatus.name)
            put(COL_REC_PREV_TAGS, serializeTags(action.previousTags))
            put(COL_REC_PREV_TIER, action.previousTier?.name)
            put(COL_REC_TIMESTAMP, action.timestamp)
        }
        db.insert(TABLE_RECENT, null, cv)
        db.execSQL("DELETE FROM $TABLE_RECENT WHERE $COL_REC_ID NOT IN (SELECT $COL_REC_ID FROM $TABLE_RECENT ORDER BY $COL_REC_ID DESC LIMIT 30)")
    }

    fun deleteRecentRating(id: Long) {
        writableDatabase.delete(TABLE_RECENT, "$COL_REC_ID = ?", arrayOf(id.toString()))
    }

    // --- RECENT TRACKER ACTIONS CRUD ---
    fun getRecentTrackerActions(limit: Int = 15): List<RecentTrackerAction> {
        val list = mutableListOf<RecentTrackerAction>()
        val db = readableDatabase
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recent_tracker_actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pokemon_id INTEGER NOT NULL,
                    pokemon_name TEXT NOT NULL,
                    prev_status TEXT NOT NULL,
                    new_status TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            val cursor = db.query("recent_tracker_actions", null, null, null, null, null, "id DESC", limit.toString())
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("id")
                val pIdIdx = it.getColumnIndexOrThrow("pokemon_id")
                val nameIdx = it.getColumnIndexOrThrow("pokemon_name")
                val prevIdx = it.getColumnIndexOrThrow("prev_status")
                val newIdx = it.getColumnIndexOrThrow("new_status")
                val timeIdx = it.getColumnIndexOrThrow("timestamp")

                while (it.moveToNext()) {
                    val prevStatus = try { TrackerStatus.valueOf(it.getString(prevIdx)) } catch (_: Exception) { TrackerStatus.UNSEEN }
                    val newStatus = try { TrackerStatus.valueOf(it.getString(newIdx)) } catch (_: Exception) { TrackerStatus.CAUGHT }
                    list.add(
                        RecentTrackerAction(
                            id = it.getLong(idIdx),
                            pokemonId = it.getInt(pIdIdx),
                            pokemonName = it.getString(nameIdx),
                            previousStatus = prevStatus,
                            newStatus = newStatus,
                            timestamp = it.getLong(timeIdx)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun addRecentTrackerAction(action: RecentTrackerAction) {
        val db = writableDatabase
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recent_tracker_actions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pokemon_id INTEGER NOT NULL,
                    pokemon_name TEXT NOT NULL,
                    prev_status TEXT NOT NULL,
                    new_status TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            val cv = ContentValues().apply {
                put("pokemon_id", action.pokemonId)
                put("pokemon_name", action.pokemonName)
                put("prev_status", action.previousStatus.name)
                put("new_status", action.newStatus.name)
                put("timestamp", action.timestamp)
            }
            db.insert("recent_tracker_actions", null, cv)
            db.execSQL("DELETE FROM recent_tracker_actions WHERE id NOT IN (SELECT id FROM recent_tracker_actions ORDER BY id DESC LIMIT 30)")
        } catch (_: Exception) {}
    }

    fun deleteRecentTrackerAction(id: Long) {
        try {
            writableDatabase.delete("recent_tracker_actions", "id = ?", arrayOf(id.toString()))
        } catch (_: Exception) {}
    }

    // --- PARTY ROSTER CRUD ---
    fun getPartySlots(): List<PartySlot> {
        val list = mutableListOf<PartySlot>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PARTY, null, null, null, null, null, "$COL_P_SLOT ASC")
        cursor.use {
            val slotIdx = it.getColumnIndexOrThrow(COL_P_SLOT)
            val pIdIdx = it.getColumnIndexOrThrow(COL_P_POKEMON_ID)
            val nickIdx = it.getColumnIndexOrThrow(COL_P_NICKNAME)
            val m1Idx = it.getColumnIndexOrThrow(COL_P_MOVE1)
            val m2Idx = it.getColumnIndexOrThrow(COL_P_MOVE2)
            val m3Idx = it.getColumnIndexOrThrow(COL_P_MOVE3)
            val m4Idx = it.getColumnIndexOrThrow(COL_P_MOVE4)
            val itemIdx = it.getColumnIndexOrThrow(COL_P_ITEM)
            val abilityIdx = it.getColumnIndexOrThrow(COL_P_ABILITY)
            val metaIdx = it.getColumnIndex(COL_P_MOVE_META)

            while (it.moveToNext()) {
                val pId = if (it.isNull(pIdIdx)) null else it.getInt(pIdIdx)
                val nick = if (it.isNull(nickIdx)) null else it.getString(nickIdx)
                val metaStr = if (metaIdx >= 0 && !it.isNull(metaIdx)) it.getString(metaIdx) else null
                val parsedMeta = parseMoveMeta(metaStr)

                list.add(
                    PartySlot(
                        slotIndex = it.getInt(slotIdx),
                        pokemonId = pId,
                        nickname = nick,
                        move1 = it.getString(m1Idx) ?: "Tackle",
                        move2 = it.getString(m2Idx) ?: "Quick Attack",
                        move3 = it.getString(m3Idx) ?: "Protect",
                        move4 = it.getString(m4Idx) ?: "Rest",
                        item = it.getString(itemIdx) ?: "Leftovers",
                        ability = it.getString(abilityIdx) ?: "Standard",
                        move1Type = parsedMeta.m1Type,
                        move1Category = parsedMeta.m1Cat,
                        move2Type = parsedMeta.m2Type,
                        move2Category = parsedMeta.m2Cat,
                        move3Type = parsedMeta.m3Type,
                        move3Category = parsedMeta.m3Cat,
                        move4Type = parsedMeta.m4Type,
                        move4Category = parsedMeta.m4Cat,
                        secretType = parsedMeta.secretType,
                        secretCategory = parsedMeta.secretCat,
                        isShiny = parsedMeta.isShiny
                    )
                )
            }
        }
        return list
    }

    fun updatePartySlot(slot: PartySlot) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_P_SLOT, slot.slotIndex)
            if (slot.pokemonId != null) put(COL_P_POKEMON_ID, slot.pokemonId) else putNull(COL_P_POKEMON_ID)
            if (slot.nickname != null) put(COL_P_NICKNAME, slot.nickname) else putNull(COL_P_NICKNAME)
            put(COL_P_MOVE1, slot.move1)
            put(COL_P_MOVE2, slot.move2)
            put(COL_P_MOVE3, slot.move3)
            put(COL_P_MOVE4, slot.move4)
            put(COL_P_ITEM, slot.item)
            put(COL_P_ABILITY, slot.ability)
            put(COL_P_MOVE_META, serializeMoveMeta(slot))
        }
        db.insertWithOnConflict(TABLE_PARTY, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private data class MoveMetaTuple(
        val m1Type: PokemonType,
        val m1Cat: String,
        val m2Type: PokemonType,
        val m2Cat: String,
        val m3Type: PokemonType,
        val m3Cat: String,
        val m4Type: PokemonType,
        val m4Cat: String,
        val secretType: PokemonType?,
        val secretCat: String,
        val isShiny: Boolean = false
    )

    private fun serializeMoveMeta(s: PartySlot): String {
        val sec = s.secretType?.name ?: "NONE"
        val shinyFlag = if (s.isShiny) "1" else "0"
        return "${s.move1Type.name}:${s.move1Category}|${s.move2Type.name}:${s.move2Category}|${s.move3Type.name}:${s.move3Category}|${s.move4Type.name}:${s.move4Category}|$sec:${s.secretCategory}|SHINY:$shinyFlag"
    }

    private fun parseMoveMeta(metaStr: String?): MoveMetaTuple {
        if (metaStr.isNullOrBlank()) {
            return MoveMetaTuple(
                PokemonType.NORMAL, "Physical",
                PokemonType.WATER, "Special",
                PokemonType.ICE, "Special",
                PokemonType.GROUND, "Physical",
                null, "Special",
                false
            )
        }
        val parts = metaStr.split("|")
        fun parsePair(idx: Int, defType: PokemonType, defCat: String): Pair<PokemonType, String> {
            val seg = parts.getOrNull(idx)?.split(":") ?: return Pair(defType, defCat)
            val t = try { PokemonType.valueOf(seg.getOrElse(0) { defType.name }) } catch (_: Exception) { defType }
            val c = seg.getOrElse(1) { defCat }
            return Pair(t, c)
        }
        val (t1, c1) = parsePair(0, PokemonType.NORMAL, "Physical")
        val (t2, c2) = parsePair(1, PokemonType.WATER, "Special")
        val (t3, c3) = parsePair(2, PokemonType.ICE, "Special")
        val (t4, c4) = parsePair(3, PokemonType.GROUND, "Physical")
        val secSeg = parts.getOrNull(4)?.split(":")
        val secTypeName = secSeg?.getOrNull(0)
        val secType = if (secTypeName == null || secTypeName == "NONE") null else try { PokemonType.valueOf(secTypeName) } catch (_: Exception) { null }
        val secCat = secSeg?.getOrElse(1) { "Special" } ?: "Special"

        val shinySeg = parts.getOrNull(5)?.split(":")
        val isShiny = shinySeg?.getOrNull(1) == "1"

        return MoveMetaTuple(t1, c1, t2, c2, t3, c3, t4, c4, secType, secCat, isShiny)
    }

    private fun serializeTags(tags: Set<RatingTag>): String = tags.joinToString(",") { it.name }

    private fun parseTags(str: String?): Set<RatingTag> {
        if (str.isNullOrBlank()) return emptySet()
        return str.split(",")
            .mapNotNull { name -> try { RatingTag.valueOf(name.trim()) } catch (_: Exception) { null } }
            .toSet()
    }
}
