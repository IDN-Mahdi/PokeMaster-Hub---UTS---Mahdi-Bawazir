package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatMessage
import com.example.ai.PartyMoveBreakdown
import com.example.ai.TacticalCoachAI
import com.example.ai.TacticalCoachAnalysis
import com.example.audio.PokemonAudioPlayer
import com.example.data.database.AppDatabaseHelper
import com.example.data.model.*
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.PokemonDatabase
import com.example.data.storage.JsonFileStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RegionalFilter(val label: String, val count: Int) {
    ALL("Pokédex", 1025),
    HGSS_JOHTO("Johto Dex", 256),
    XY_KALOS("Kalos Dex", 457),
    PLA_HISUI("Hisui Dex", 242),
    GEN_1("Gen 1", 151),
    GEN_2("Gen 2", 100),
    GEN_3("Gen 3", 135),
    GEN_4("Gen 4", 107),
    GEN_5("Gen 5", 156),
    GEN_6("Gen 6", 72),
    GEN_7("Gen 7", 88),
    GEN_8("Gen 8", 96),
    GEN_9("Gen 9", 120)
}

enum class PokedexPageMode(val label: String) {
    LOVE_EM_ALL_PICKER("Picker"),
    CATCH_EM_ALL_TRACKER("Tracker")
}

enum class RatingFilterOption(val label: String) {
    ALL("All Pokémon"),
    ALL_LIKED("All Liked"),
    LIKED_ICON("Liked: Iconic (Gold)"),
    LIKED_DESIGN("Liked: Design (White/Platinum)"),
    LIKED_PARTY("Liked: Party Viability (Red)"),
    DISLIKED_ONLY("Disliked Only"),
    UNRATED_ONLY("Unrated Only")
}

data class ProfileBadgeSettings(
    val showLegendaryMythical: Boolean = true,
    val showNoobTrap: Boolean = true,
    val showSingularNoEvo: Boolean = true,
    val showTierRating: Boolean = true,
    val showBst: Boolean = true
)

enum class JohtoSubTab(val title: String) {
    FAMILIES("Evolution Families"),
    TYPE_SPECIALISTS("Type Specialists"),
    TRACKER("Pokédex Tracker"),
    FREE_RANKER("Top 50 Ranker")
}

data class TypeDefenseStat(
    val type: PokemonType,
    val weak4xCount: Int,
    val weak2xCount: Int,
    val resistOrImmuneCount: Int
) {
    val totalWeak: Int get() = weak4xCount + weak2xCount
}

class PokemonViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = AppDatabaseHelper(application)
    private val prefs = UserPreferencesManager(application)
    private val storage = JsonFileStorageManager(application)
    private val audioPlayer = PokemonAudioPlayer(application)

    val allPokemonList: List<Pokemon> = PokemonDatabase.allPokemon
    val pokeMap: Map<Int, Pokemon> = allPokemonList.associateBy { it.id }

    // Persistent State
    private val _ratings = MutableStateFlow<Map<Int, PokemonRating>>(emptyMap())
    val ratings: StateFlow<Map<Int, PokemonRating>> = _ratings.asStateFlow()

    private val _tracker = MutableStateFlow<Map<Int, PokemonTrackerEntry>>(emptyMap())
    val tracker: StateFlow<Map<Int, PokemonTrackerEntry>> = _tracker.asStateFlow()

    private val _recentRatings = MutableStateFlow<List<RecentRatingAction>>(emptyList())
    val recentRatings: StateFlow<List<RecentRatingAction>> = _recentRatings.asStateFlow()

    private val _recentTrackerActions = MutableStateFlow<List<RecentTrackerAction>>(emptyList())
    val recentTrackerActions: StateFlow<List<RecentTrackerAction>> = _recentTrackerActions.asStateFlow()

    private val _partySlots = MutableStateFlow<List<PartySlot>>(emptyList())
    val partySlots: StateFlow<List<PartySlot>> = _partySlots.asStateFlow()

    // Preferences & Filter State
    private val _isModern = MutableStateFlow(prefs.isModern)
    val isModern: StateFlow<Boolean> = _isModern.asStateFlow()

    private val _isShinyGlobal = MutableStateFlow(prefs.isShinyGlobal)
    val isShinyGlobal: StateFlow<Boolean> = _isShinyGlobal.asStateFlow()

    private val _preferredShinyIds = MutableStateFlow<Set<Int>>(emptySet())
    val preferredShinyIds: StateFlow<Set<Int>> = _preferredShinyIds.asStateFlow()

    private val _quizStreak = MutableStateFlow(prefs.quizStreak)
    val quizStreak: StateFlow<Int> = _quizStreak.asStateFlow()

    private val _bestStreak = MutableStateFlow(prefs.bestStreak)
    val bestStreak: StateFlow<Int> = _bestStreak.asStateFlow()

    private val _regionalFilter = MutableStateFlow(RegionalFilter.ALL)
    val regionalFilter: StateFlow<RegionalFilter> = _regionalFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<PokemonType?>(null)
    val typeFilter: StateFlow<PokemonType?> = _typeFilter.asStateFlow()

    private val _tierFilter = MutableStateFlow<Tier?>(null)
    val tierFilter: StateFlow<Tier?> = _tierFilter.asStateFlow()

    private val _trapFilterOnly = MutableStateFlow(false)
    val trapFilterOnly: StateFlow<Boolean> = _trapFilterOnly.asStateFlow()

    private val _isSingleDexView = MutableStateFlow(true)
    val isSingleDexView: StateFlow<Boolean> = _isSingleDexView.asStateFlow()

    private val _likedOnlyFilter = MutableStateFlow(false)
    val likedOnlyFilter: StateFlow<Boolean> = _likedOnlyFilter.asStateFlow()

    private val _typeEra = MutableStateFlow(TypeEra.GEN_6_9_18)
    val typeEra: StateFlow<TypeEra> = _typeEra.asStateFlow()

    private val _ratingFilter = MutableStateFlow(RatingFilterOption.ALL)
    val ratingFilter: StateFlow<RatingFilterOption> = _ratingFilter.asStateFlow()

    private val _pokedexPageMode = MutableStateFlow(PokedexPageMode.LOVE_EM_ALL_PICKER)
    val pokedexPageMode: StateFlow<PokedexPageMode> = _pokedexPageMode.asStateFlow()

    private val _profileBadgeSettings = MutableStateFlow(ProfileBadgeSettings())
    val profileBadgeSettings: StateFlow<ProfileBadgeSettings> = _profileBadgeSettings.asStateFlow()

    // Modals and Details
    private val _detailPokemon = MutableStateFlow<Pokemon?>(null)
    val detailPokemon: StateFlow<Pokemon?> = _detailPokemon.asStateFlow()

    private val _isPlayingCry = MutableStateFlow(false)
    val isPlayingCry: StateFlow<Boolean> = _isPlayingCry.asStateFlow()

    private val _showRecentModal = MutableStateFlow(false)
    val showRecentModal: StateFlow<Boolean> = _showRecentModal.asStateFlow()

    private val _showExportImportModal = MutableStateFlow(false)
    val showExportImportModal: StateFlow<Boolean> = _showExportImportModal.asStateFlow()

    private val _showFlutterSourceModal = MutableStateFlow(false)
    val showFlutterSourceModal: StateFlow<Boolean> = _showFlutterSourceModal.asStateFlow()

    // Live REST API Integration (PokéAPI v2)
    val pokeApiService = com.example.data.api.PokeApiService(application)
    private val _liveDetailsMap = MutableStateFlow<Map<Int, com.example.data.api.PokemonLiveDetail>>(emptyMap())
    val liveDetailsMap: StateFlow<Map<Int, com.example.data.api.PokemonLiveDetail>> = _liveDetailsMap.asStateFlow()

    private val _isLiveSyncing = MutableStateFlow(false)
    val isLiveSyncing: StateFlow<Boolean> = _isLiveSyncing.asStateFlow()

    val liveApiStatus = "Live API Connected: pokeapi.co/api/v2"

    // Tournament & Trainer Passport
    private val _ultimateFavorite = MutableStateFlow<Pokemon?>(null)
    val ultimateFavorite: StateFlow<Pokemon?> = _ultimateFavorite.asStateFlow()

    // Quiz Accuracy
    private val _quizTotal = MutableStateFlow(0)
    val quizTotal: StateFlow<Int> = _quizTotal.asStateFlow()

    private val _quizCorrect = MutableStateFlow(0)
    val quizCorrect: StateFlow<Int> = _quizCorrect.asStateFlow()

    // Tab 2 Johto Curator State
    private val _johtoSubTab = MutableStateFlow(JohtoSubTab.FAMILIES)
    val johtoSubTab: StateFlow<JohtoSubTab> = _johtoSubTab.asStateFlow()

    private val _typeAssignments = MutableStateFlow<Map<PokemonType, List<Int>>>(emptyMap())
    val typeAssignments: StateFlow<Map<PokemonType, List<Int>>> = _typeAssignments.asStateFlow()

    private val _typeQuotas = MutableStateFlow<Map<PokemonType, Int>>(emptyMap())
    val typeQuotas: StateFlow<Map<PokemonType, Int>> = _typeQuotas.asStateFlow()

    private val _freeRankerTop50 = MutableStateFlow<List<Int>>(emptyList())
    val freeRankerTop50: StateFlow<List<Int>> = _freeRankerTop50.asStateFlow()

    private val _preferredStages = MutableStateFlow<Map<String, Int>>(emptyMap())
    val preferredStages: StateFlow<Map<String, Int>> = _preferredStages.asStateFlow()

    // AI Tactical Coach state & Live 2-Way Chat
    private val _tacticalAnalysis = MutableStateFlow<TacticalCoachAnalysis?>(null)
    val tacticalAnalysis: StateFlow<TacticalCoachAnalysis?> = _tacticalAnalysis.asStateFlow()

    private val _isAnalyzingAI = MutableStateFlow(false)
    val isAnalyzingAI: StateFlow<Boolean> = _isAnalyzingAI.asStateFlow()

    private val _aiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                isUser = false,
                senderLabel = "AI Tactical Coach",
                message = "Halo Trainer! Saya memantau ke-6 slot Party Anda secara real-time, termasuk konfigurasi 4 Move (Elemental Type + Physical/Special) dan Secret Type. Tanyakan analisis kelemahan tim, celah cakupan serangan (blind spots), atau rekomendasi Secret Type kapan saja."
            )
        )
    )
    val aiChatMessages: StateFlow<List<AiChatMessage>> = _aiChatMessages.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _ratings.value = dbHelper.getAllRatings()
            _tracker.value = dbHelper.getAllTrackerEntries()
            _recentRatings.value = dbHelper.getRecentRatings(15)
            _recentTrackerActions.value = dbHelper.getRecentTrackerActions(15)
            _partySlots.value = dbHelper.getPartySlots()
            _preferredShinyIds.value = prefs.getPreferredShinyIds()

            // Load Johto Mode A Quotas and Assignments
            val quotaMap = mutableMapOf<PokemonType, Int>()
            val assignMap = mutableMapOf<PokemonType, List<Int>>()
            PokemonType.entries.forEach { type ->
                quotaMap[type] = prefs.getTypeQuota(type)
                assignMap[type] = prefs.getTypeAssignments(type)
            }
            _typeQuotas.value = quotaMap
            _typeAssignments.value = assignMap

            // Load Top 50 Free Ranker
            var top50 = prefs.getFreeRankerTop50()
            if (top50.isEmpty()) {
                top50 = PokemonDatabase.johtoDexPokemon.sortedByDescending { it.bst }.take(50).map { it.id }
                prefs.setFreeRankerTop50(top50)
            }
            _freeRankerTop50.value = top50

            triggerAutosave()
        }
    }

    private fun triggerAutosave() {
        viewModelScope.launch {
            storage.saveToFile(
                ratings = _ratings.value,
                tracker = _tracker.value,
                party = _partySlots.value,
                mechanicMode = if (_isModern.value) "modern" else "gen4",
                quizStreak = _quizStreak.value,
                bestStreak = _bestStreak.value
            )
        }
    }

    // --- RATING ACTIONS ---
    fun ratePokemon(id: Int, status: RatingStatus, specificTag: RatingTag? = null) {
        val mon = pokeMap[id] ?: return
        val current = _ratings.value[id] ?: PokemonRating(id)

        val action = RecentRatingAction(
            pokemonId = id,
            pokemonName = mon.name,
            actionType = if (status == RatingStatus.LIKE) "LIKE" else "DISLIKE",
            previousStatus = current.status,
            previousTags = current.tags,
            previousTier = current.customTier
        )

        val newTags = if (status == RatingStatus.LIKE) {
            if (specificTag != null) {
                if (current.tags.contains(specificTag)) current.tags - specificTag else current.tags + specificTag
            } else {
                if (current.tags.isEmpty()) setOf(RatingTag.ICON) else current.tags
            }
        } else {
            emptySet()
        }

        val updatedRating = current.copy(
            status = status,
            tags = newTags,
            updatedAt = System.currentTimeMillis()
        )

        val newRatings = _ratings.value.toMutableMap()
        newRatings[id] = updatedRating
        _ratings.value = newRatings

        dbHelper.upsertRating(updatedRating)
        dbHelper.addRecentRating(action)
        _recentRatings.value = dbHelper.getRecentRatings(15)

        triggerAutosave()
    }

    fun toggleTag(id: Int, tag: RatingTag) {
        val current = _ratings.value[id] ?: PokemonRating(id)

        val newTags = if (current.tags.contains(tag)) {
            current.tags - tag
        } else {
            current.tags + tag
        }

        val newStatus = if (newTags.isNotEmpty()) RatingStatus.LIKE else current.status

        val updated = current.copy(
            status = newStatus,
            tags = newTags,
            updatedAt = System.currentTimeMillis()
        )

        val newRatings = _ratings.value.toMutableMap()
        newRatings[id] = updated
        _ratings.value = newRatings

        dbHelper.upsertRating(updated)
        triggerAutosave()
    }

    fun setCustomTier(id: Int, tier: Tier?) {
        val current = _ratings.value[id] ?: PokemonRating(id)
        val updated = current.copy(customTier = tier, updatedAt = System.currentTimeMillis())

        val newRatings = _ratings.value.toMutableMap()
        newRatings[id] = updated
        _ratings.value = newRatings

        dbHelper.upsertRating(updated)
        triggerAutosave()
    }

    fun undoLastRating(action: RecentRatingAction) {
        val restored = PokemonRating(
            pokemonId = action.pokemonId,
            status = action.previousStatus,
            tags = action.previousTags,
            customTier = action.previousTier,
            updatedAt = System.currentTimeMillis()
        )
        val newRatings = _ratings.value.toMutableMap()
        newRatings[action.pokemonId] = restored
        _ratings.value = newRatings

        dbHelper.upsertRating(restored)
        dbHelper.deleteRecentRating(action.id)
        _recentRatings.value = dbHelper.getRecentRatings(15)

        triggerAutosave()
    }

    fun resetRating(id: Int) {
        val current = _ratings.value[id] ?: return
        val mon = pokeMap[id] ?: return

        val action = RecentRatingAction(
            pokemonId = id,
            pokemonName = mon.name,
            actionType = "RESET",
            previousStatus = current.status,
            previousTags = current.tags,
            previousTier = current.customTier
        )

        val updated = PokemonRating(id, RatingStatus.UNRATED, emptySet(), null)
        val newRatings = _ratings.value.toMutableMap()
        newRatings[id] = updated
        _ratings.value = newRatings

        dbHelper.upsertRating(updated)
        dbHelper.addRecentRating(action)
        _recentRatings.value = dbHelper.getRecentRatings(15)

        triggerAutosave()
    }

    fun batchTagFavorites(tag: RatingTag) {
        val newRatings = _ratings.value.toMutableMap()
        _ratings.value.forEach { (id, rating) ->
            if (rating.status == RatingStatus.LIKE) {
                val updated = rating.copy(tags = setOf(tag), updatedAt = System.currentTimeMillis())
                newRatings[id] = updated
                dbHelper.upsertRating(updated)
            }
        }
        _ratings.value = newRatings
        triggerAutosave()
    }

    // --- TRACKER ACTIONS ---
    fun setTrackerStatus(id: Int, newStatus: TrackerStatus) {
        val prev = _tracker.value[id]?.status ?: TrackerStatus.UNSEEN
        if (prev != newStatus) {
            val mon = pokeMap[id]
            val action = RecentTrackerAction(
                pokemonId = id,
                pokemonName = mon?.name ?: "#$id",
                previousStatus = prev,
                newStatus = newStatus
            )
            dbHelper.addRecentTrackerAction(action)
            _recentTrackerActions.value = dbHelper.getRecentTrackerActions(15)
        }

        val entry = PokemonTrackerEntry(id, newStatus, System.currentTimeMillis())
        val newTracker = _tracker.value.toMutableMap()
        newTracker[id] = entry
        _tracker.value = newTracker

        dbHelper.upsertTrackerEntry(entry)
        triggerAutosave()
    }

    fun undoTrackerAction(action: RecentTrackerAction) {
        val restored = PokemonTrackerEntry(action.pokemonId, action.previousStatus, System.currentTimeMillis())
        val newTracker = _tracker.value.toMutableMap()
        newTracker[action.pokemonId] = restored
        _tracker.value = newTracker

        dbHelper.upsertTrackerEntry(restored)
        dbHelper.deleteRecentTrackerAction(action.id)
        _recentTrackerActions.value = dbHelper.getRecentTrackerActions(15)
        triggerAutosave()
    }

    // --- PARTY ROSTER ACTIONS ---
    fun togglePartySlotShiny(slotIndex: Int) {
        val currentSlots = _partySlots.value.toMutableList()
        if (slotIndex in currentSlots.indices) {
            val current = currentSlots[slotIndex]
            val updated = current.copy(isShiny = !current.isShiny)
            currentSlots[slotIndex] = updated
            dbHelper.updatePartySlot(updated)
            _partySlots.value = currentSlots
            triggerAutosave()
        }
    }

    private fun createConfiguredSlotForPokemon(existing: PartySlot, mon: Pokemon?): PartySlot {
        if (mon == null) {
            return existing.copy(pokemonId = null, nickname = null)
        }
        val prefCat = if (mon.spAtk > mon.attack) "Special" else "Physical"
        val m1T = mon.primaryType
        val m2T = mon.secondaryType ?: if (mon.primaryType == PokemonType.GROUND) PokemonType.ROCK else PokemonType.GROUND
        val m3T = if (mon.primaryType == PokemonType.ICE || mon.secondaryType == PokemonType.ICE) PokemonType.ELECTRIC else PokemonType.ICE
        val m4T = if (mon.primaryType == PokemonType.FIRE || mon.secondaryType == PokemonType.FIRE) PokemonType.FIGHTING else PokemonType.FIRE
        return existing.copy(
            pokemonId = mon.id,
            nickname = mon.name,
            item = existing.item,
            move1Type = m1T,
            move2Type = m2T,
            move3Type = m3T,
            move4Type = m4T,
            move1Category = prefCat,
            move2Category = prefCat,
            move3Category = "Special",
            move4Category = prefCat,
            isShiny = existing.isShiny
        )
    }

    fun assignPokemonToPartySlot(slotIndex: Int, pokemonId: Int?) {
        val currentSlots = _partySlots.value.toMutableList()
        if (slotIndex !in currentSlots.indices) return
        val mon = pokemonId?.let { pokeMap[it] }
        val updated = createConfiguredSlotForPokemon(currentSlots[slotIndex], mon)
        currentSlots[slotIndex] = updated
        dbHelper.updatePartySlot(updated)
        _partySlots.value = currentSlots
        triggerAutosave()
    }

    fun togglePartyMembership(pokemonId: Int) {
        val currentSlots = _partySlots.value.toMutableList()
        val existingSlotIndex = currentSlots.indexOfFirst { it.pokemonId == pokemonId }

        if (existingSlotIndex != -1) {
            val slot = currentSlots[existingSlotIndex].copy(pokemonId = null, nickname = null)
            currentSlots[existingSlotIndex] = slot
            dbHelper.updatePartySlot(slot)
            _partySlots.value = currentSlots
        } else {
            val emptySlotIndex = currentSlots.indexOfFirst { it.pokemonId == null }
            if (emptySlotIndex != -1) {
                val mon = pokeMap[pokemonId]
                val slot = createConfiguredSlotForPokemon(currentSlots[emptySlotIndex], mon)
                currentSlots[emptySlotIndex] = slot
                dbHelper.updatePartySlot(slot)
                _partySlots.value = currentSlots
            }
        }
        triggerAutosave()
    }

    fun updatePartySlot(slot: PartySlot) {
        val currentSlots = _partySlots.value.toMutableList()
        if (slot.slotIndex in currentSlots.indices) {
            currentSlots[slot.slotIndex] = slot
            dbHelper.updatePartySlot(slot)
            _partySlots.value = currentSlots
            triggerAutosave()
        }
    }

    // --- JOHTO MODE A & B ---
    fun setTypeQuota(type: PokemonType, quota: Int) {
        prefs.setTypeQuota(type, quota)
        val newQuotas = _typeQuotas.value.toMutableMap()
        newQuotas[type] = quota
        _typeQuotas.value = newQuotas

        // Trim existing assignments if exceeding new quota
        val current = _typeAssignments.value[type] ?: emptyList()
        if (current.size > quota) {
            val trimmed = current.take(quota)
            prefs.setTypeAssignments(type, trimmed)
            val newMap = _typeAssignments.value.toMutableMap()
            newMap[type] = trimmed
            _typeAssignments.value = newMap
        }
    }

    fun setBatchTypeQuota(quota: Int) {
        prefs.setBatchTypeQuota(quota)
        val newQuotas = mutableMapOf<PokemonType, Int>()
        PokemonType.entries.forEach { newQuotas[it] = quota }
        _typeQuotas.value = newQuotas
    }

    fun applyQuickSetAllTypes(topN: Int) {
        setBatchTypeQuota(topN)
        val johtoPool = PokemonDatabase.johtoDexPokemon
        val newAssignments = mutableMapOf<PokemonType, List<Int>>()
        PokemonType.entries.forEach { type ->
            val topMonsForType = johtoPool
                .filter { it.primaryType == type || it.secondaryType == type }
                .sortedByDescending { it.bst }
                .take(topN)
                .map { it.id }
            newAssignments[type] = topMonsForType
            prefs.setTypeAssignments(type, topMonsForType)
        }
        _typeAssignments.value = newAssignments
    }

    fun clearAllTypeSpecialists() {
        val emptyMap = mutableMapOf<PokemonType, List<Int>>()
        PokemonType.entries.forEach { type ->
            emptyMap[type] = emptyList()
            prefs.setTypeAssignments(type, emptyList())
        }
        _typeAssignments.value = emptyMap
    }

    fun assignToTypeSpecialist(type: PokemonType, pokemonId: Int) {
        val quota = _typeQuotas.value[type] ?: 3
        val current = (_typeAssignments.value[type] ?: emptyList()).toMutableList()

        if (!current.contains(pokemonId) && current.size < quota) {
            current.add(pokemonId)
            prefs.setTypeAssignments(type, current)

            val newMap = _typeAssignments.value.toMutableMap()
            newMap[type] = current
            _typeAssignments.value = newMap
        }
    }

    fun removeFromTypeSpecialist(type: PokemonType, pokemonId: Int) {
        val current = (_typeAssignments.value[type] ?: emptyList()).toMutableList()
        if (current.remove(pokemonId)) {
            prefs.setTypeAssignments(type, current)
            val newMap = _typeAssignments.value.toMutableMap()
            newMap[type] = current
            _typeAssignments.value = newMap
        }
    }

    fun reorderTypeSpecialist(type: PokemonType, fromIndex: Int, toIndex: Int) {
        val current = (_typeAssignments.value[type] ?: emptyList()).toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            prefs.setTypeAssignments(type, current)
            val newMap = _typeAssignments.value.toMutableMap()
            newMap[type] = current
            _typeAssignments.value = newMap
        }
    }

    fun moveToSecondaryType(currentType: PokemonType, pokemonId: Int) {
        val mon = pokeMap[pokemonId] ?: return
        val targetType = if (mon.primaryType == currentType) mon.secondaryType else mon.primaryType
        if (targetType != null) {
            removeFromTypeSpecialist(currentType, pokemonId)
            assignToTypeSpecialist(targetType, pokemonId)
        }
    }

    fun reorderFreeRanker(fromIndex: Int, toIndex: Int) {
        val current = _freeRankerTop50.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _freeRankerTop50.value = current
            prefs.setFreeRankerTop50(current)
        }
    }

    fun setPreferredStage(familyId: String, pokemonId: Int) {
        prefs.setPreferredStage(familyId, pokemonId)
        val newMap = _preferredStages.value.toMutableMap()
        newMap[familyId] = pokemonId
        _preferredStages.value = newMap
    }

    // --- CONTROLS & MODALS ---
    fun setRegionalFilter(filter: RegionalFilter) {
        _regionalFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: PokemonType?) {
        _typeFilter.value = type
    }

    fun setTierFilter(tier: Tier?) {
        _tierFilter.value = tier
    }

    fun toggleTrapFilter() {
        _trapFilterOnly.value = !_trapFilterOnly.value
    }

    fun toggleSingleDexView() {
        _isSingleDexView.value = !_isSingleDexView.value
    }

    fun toggleLikedOnlyFilter() {
        _likedOnlyFilter.value = !_likedOnlyFilter.value
    }

    fun setPokedexPageMode(mode: PokedexPageMode) {
        _pokedexPageMode.value = mode
    }

    fun updateProfileBadgeSettings(settings: ProfileBadgeSettings) {
        _profileBadgeSettings.value = settings
    }

    fun setTypeEra(era: TypeEra) {
        _typeEra.value = era
        val isModernMode = (era == TypeEra.ALL_TYPES || era == TypeEra.GEN_6_9_18)
        _isModern.value = isModernMode
        prefs.mechanicMode = if (isModernMode) "modern" else "gen4"
        triggerAutosave()
    }

    fun setRatingFilter(option: RatingFilterOption) {
        _ratingFilter.value = option
    }

    fun toggleRatingTag(id: Int, tag: RatingTag) {
        val current = _ratings.value[id] ?: PokemonRating(id)
        val currentTags = current.tags
        val isCurrentlyDisliked = current.status == RatingStatus.DISLIKE

        val newTags = if (isCurrentlyDisliked) {
            setOf(tag)
        } else if (currentTags.contains(tag)) {
            currentTags - tag
        } else {
            currentTags + tag
        }

        val newStatus = if (newTags.isEmpty()) RatingStatus.UNRATED else RatingStatus.LIKE
        val updated = current.copy(
            status = newStatus,
            tags = newTags,
            updatedAt = System.currentTimeMillis()
        )
        val newRatings = _ratings.value.toMutableMap()
        newRatings[id] = updated
        _ratings.value = newRatings
        dbHelper.upsertRating(updated)
        triggerAutosave()
    }

    fun markDislike(id: Int) {
        ratePokemon(id, RatingStatus.DISLIKE, null)
    }

    fun toggleMechanicMode() {
        val next = !_isModern.value
        _isModern.value = next
        prefs.mechanicMode = if (next) "modern" else "gen4"
        _typeEra.value = if (next) TypeEra.GEN_6_9_18 else TypeEra.GEN_2_5_17
        triggerAutosave()
    }

    fun toggleShinyGlobal() {
        val next = !_isShinyGlobal.value
        _isShinyGlobal.value = next
        prefs.isShinyGlobal = next
    }

    fun togglePreferredShiny(pokemonId: Int, isShiny: Boolean) {
        val current = _preferredShinyIds.value.toMutableSet()
        if (isShiny) {
            current.add(pokemonId)
        } else {
            current.remove(pokemonId)
        }
        _preferredShinyIds.value = current
        prefs.setPreferredShinyIds(current)
    }

    fun setJohtoSubTab(tab: JohtoSubTab) {
        _johtoSubTab.value = tab
    }

    fun openDetailModal(pokemon: Pokemon) {
        _detailPokemon.value = pokemon
        playAudioCry(pokemon.id)
    }

    fun closeDetailModal() {
        _detailPokemon.value = null
        audioPlayer.stop()
        _isPlayingCry.value = false
    }

    fun playAudioCry(pokemonId: Int) {
        viewModelScope.launch {
            _isPlayingCry.value = true
            audioPlayer.playCry(pokemonId) {
                _isPlayingCry.value = false
            }
        }
    }

    fun openRecentModal(show: Boolean) {
        _showRecentModal.value = show
    }

    fun openExportImportModal(show: Boolean) {
        _showExportImportModal.value = show
    }

    fun openFlutterSourceModal(show: Boolean) {
        _showFlutterSourceModal.value = show
    }

    fun setUltimateFavorite(pokemon: Pokemon) {
        _ultimateFavorite.value = pokemon
    }

    fun fetchPokemonLiveDetails(id: Int, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLiveSyncing.value = true
            try {
                val detail = pokeApiService.fetchPokemonDetails(id)
                val newMap = _liveDetailsMap.value.toMutableMap()
                newMap[id] = detail
                _liveDetailsMap.value = newMap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLiveSyncing.value = false
            }
        }
    }

    // Cumulative Defensive Weakness Calculation
    fun calculateTeamCumulativeWeaknesses(): Map<PokemonType, Int> {
        val active = _partySlots.value.mapNotNull { it.pokemonId?.let { id -> pokeMap[id] } }
        val allTypes = PokemonType.activeTypes(_isModern.value)
        val weaknessCount = mutableMapOf<PokemonType, Int>()

        allTypes.forEach { attackType ->
            var count = 0
            active.forEach { mon ->
                val mult = PokemonType.getDefendingMultiplier(attackType, mon.primaryType, mon.secondaryType, _isModern.value)
                if (mult >= 2.0f) count++
            }
            if (count > 0) weaknessCount[attackType] = count
        }
        return weaknessCount
    }

    fun calculateTeamDefensiveProfile(): List<TypeDefenseStat> {
        val active = _partySlots.value.mapNotNull { it.pokemonId?.let { id -> pokeMap[id] } }
        val allTypes = PokemonType.activeTypes(_isModern.value)
        return allTypes.map { atkType ->
            var w4 = 0
            var w2 = 0
            var res = 0
            active.forEach { mon ->
                val mult = PokemonType.getDefendingMultiplier(atkType, mon.primaryType, mon.secondaryType, _isModern.value)
                when {
                    mult >= 4.0f -> w4++
                    mult >= 2.0f -> w2++
                    mult < 1.0f -> res++
                }
            }
            TypeDefenseStat(atkType, w4, w2, res)
        }
    }

    // Offensive Move Coverage Calculation strictly from 4 Moves + Secret Type
    fun calculateDetailedOffensiveCoverage(): PartyMoveBreakdown {
        return TacticalCoachAI.computeMoveCoverageBreakdown(
            party = _partySlots.value,
            pokeMap = pokeMap,
            isModern = _isModern.value
        )
    }

    fun calculateOffensiveCoverage(): Pair<Int, Set<PokemonType>> {
        val breakdown = calculateDetailedOffensiveCoverage()
        return Pair(breakdown.coveredTypes.size, breakdown.coveredTypes)
    }

    fun recordQuizAnswer(correct: Boolean) {
        _quizTotal.value = _quizTotal.value + 1
        if (correct) {
            _quizCorrect.value = _quizCorrect.value + 1
            val newStreak = _quizStreak.value + 1
            _quizStreak.value = newStreak
            prefs.quizStreak = newStreak
            if (newStreak > _bestStreak.value) {
                _bestStreak.value = newStreak
                prefs.bestStreak = newStreak
            }
        } else {
            _quizStreak.value = 0
            prefs.quizStreak = 0
        }
    }

    fun sendAiCoachMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = AiChatMessage(
            isUser = true,
            senderLabel = "You",
            message = trimmed
        )
        _aiChatMessages.value = _aiChatMessages.value + userMsg

        viewModelScope.launch {
            _isAnalyzingAI.value = true
            val replyText = TacticalCoachAI.generateInteractiveChatResponse(
                userQuery = trimmed,
                party = _partySlots.value,
                allPokemon = allPokemonList,
                isModern = _isModern.value
            )
            val aiMsg = AiChatMessage(
                isUser = false,
                senderLabel = "AI Tactical Coach",
                message = replyText
            )
            _aiChatMessages.value = _aiChatMessages.value + aiMsg
            _isAnalyzingAI.value = false
        }
    }

    fun requestAiCoachQuickPrompt(promptType: String) {
        sendAiCoachMessage(promptType)
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            val analysis = TacticalCoachAI.analyzeParty(_partySlots.value, allPokemonList, _isModern.value)
            _tacticalAnalysis.value = analysis
            _isAnalyzingAI.value = false
        }
    }

    fun runAiTacticalAnalysis() {
        viewModelScope.launch {
            _isAnalyzingAI.value = true
            val analysis = TacticalCoachAI.analyzeParty(
                party = _partySlots.value,
                allPokemon = allPokemonList,
                isModern = _isModern.value
            )
            _tacticalAnalysis.value = analysis
            _isAnalyzingAI.value = false
        }
    }

    // --- EXPORT & IMPORT ---
    fun exportJsonString(): String {
        return storage.exportToJson(
            ratings = _ratings.value,
            tracker = _tracker.value,
            party = _partySlots.value,
            mechanicMode = if (_isModern.value) "modern" else "gen4"
        )
    }

    fun importJsonString(json: String): Boolean {
        val result = storage.importFromJson(json) ?: return false
        viewModelScope.launch {
            val newRatings = _ratings.value.toMutableMap()
            result.ratings.forEach {
                newRatings[it.pokemonId] = it
                dbHelper.upsertRating(it)
            }
            _ratings.value = newRatings

            val newTracker = _tracker.value.toMutableMap()
            result.tracker.forEach {
                newTracker[it.pokemonId] = it
                dbHelper.upsertTrackerEntry(it)
            }
            _tracker.value = newTracker

            if (result.party.isNotEmpty()) {
                result.party.forEach { dbHelper.updatePartySlot(it) }
                _partySlots.value = dbHelper.getPartySlots()
            }

            if (result.mechanicMode != null) {
                val isMod = result.mechanicMode == "modern"
                _isModern.value = isMod
                prefs.mechanicMode = result.mechanicMode
            }

            triggerAutosave()
        }
        return true
    }

    fun exportMarkdownSummary(): String {
        return storage.exportToMarkdownSummary(
            ratings = _ratings.value,
            tracker = _tracker.value,
            party = _partySlots.value,
            allPokemon = allPokemonList
        )
    }

    fun exportShowdown(): String {
        return storage.exportToShowdownFormat(
            party = _partySlots.value,
            allPokemon = allPokemonList
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
