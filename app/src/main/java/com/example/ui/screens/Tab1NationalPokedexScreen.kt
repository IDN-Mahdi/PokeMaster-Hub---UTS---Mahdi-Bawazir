package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.PokemonDatabase
import com.example.ui.PokedexPageMode
import com.example.ui.PokemonViewModel
import com.example.ui.ProfileBadgeSettings
import com.example.ui.RatingFilterOption
import com.example.ui.RegionalFilter
import com.example.ui.components.PokemonCard
import com.example.ui.components.TypePill
import com.example.ui.theme.*

@Composable
fun Tab1NationalPokedexScreen(
    viewModel: PokemonViewModel,
    modifier: Modifier = Modifier
) {
    val ratings by viewModel.ratings.collectAsState()
    val tracker by viewModel.tracker.collectAsState()
    val partySlots by viewModel.partySlots.collectAsState()
    val isShiny by viewModel.isShinyGlobal.collectAsState()
    val regionalFilter by viewModel.regionalFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val typeEra by viewModel.typeEra.collectAsState()
    val ratingFilter by viewModel.ratingFilter.collectAsState()
    val isSingleDexView by viewModel.isSingleDexView.collectAsState()
    val pageMode by viewModel.pokedexPageMode.collectAsState()
    val badgeSettings by viewModel.profileBadgeSettings.collectAsState()
    val preferredStages by viewModel.preferredStages.collectAsState()
    val preferredShinyIds by viewModel.preferredShinyIds.collectAsState()

    var showDsMenu by remember { mutableStateOf(false) }
    var showDexMenu by remember { mutableStateOf(false) }
    var showGensMenu by remember { mutableStateOf(false) }
    var showTypesMenu by remember { mutableStateOf(false) }
    var showRatingBatchMenu by remember { mutableStateOf(false) }
    var trackerFilterMode by remember { mutableStateOf(TrackerFilterMode.ALL) }
    var gridColumns by remember { mutableIntStateOf(2) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val isEvolutionFamilyMode = !isSingleDexView

    val partyIds = remember(partySlots) {
        partySlots.mapNotNull { it.pokemonId }.toSet()
    }

    // Helper for active Dex number display
    fun getActiveDexNumber(mon: Pokemon, filter: RegionalFilter): Int {
        return when (filter) {
            RegionalFilter.HGSS_JOHTO -> mon.johtoDexNo ?: mon.id
            RegionalFilter.XY_KALOS -> mon.kalosDexNo ?: mon.id
            RegionalFilter.PLA_HISUI -> mon.hisuiDexNo ?: mon.id
            else -> mon.id
        }
    }

    fun formatActiveDexNumber(mon: Pokemon, filter: RegionalFilter): String {
        return when (filter) {
            RegionalFilter.HGSS_JOHTO -> String.format("#%03d", mon.johtoDexNo ?: mon.id)
            RegionalFilter.XY_KALOS -> String.format("#%03d", mon.kalosDexNo ?: mon.id)
            RegionalFilter.PLA_HISUI -> String.format("#%03d", mon.hisuiDexNo ?: mon.id)
            else -> mon.formattedId()
        }
    }

    // 1. Base pool filtered by Dex / Generation
    val baseDexPool = remember(regionalFilter) {
        val raw = when (regionalFilter) {
            RegionalFilter.ALL -> viewModel.allPokemonList
            RegionalFilter.HGSS_JOHTO -> viewModel.allPokemonList.filter { it.johtoDexNo != null }
            RegionalFilter.XY_KALOS -> viewModel.allPokemonList.filter { it.kalosDexNo != null }
            RegionalFilter.PLA_HISUI -> viewModel.allPokemonList.filter { it.hisuiDexNo != null }
            RegionalFilter.GEN_1 -> viewModel.allPokemonList.filter { it.generation == 1 }
            RegionalFilter.GEN_2 -> viewModel.allPokemonList.filter { it.generation == 2 }
            RegionalFilter.GEN_3 -> viewModel.allPokemonList.filter { it.generation == 3 }
            RegionalFilter.GEN_4 -> viewModel.allPokemonList.filter { it.generation == 4 }
            RegionalFilter.GEN_5 -> viewModel.allPokemonList.filter { it.generation == 5 }
            RegionalFilter.GEN_6 -> viewModel.allPokemonList.filter { it.generation == 6 }
            RegionalFilter.GEN_7 -> viewModel.allPokemonList.filter { it.generation == 7 }
            RegionalFilter.GEN_8 -> viewModel.allPokemonList.filter { it.generation == 8 }
            RegionalFilter.GEN_9 -> viewModel.allPokemonList.filter { it.generation == 9 }
        }
        raw.sortedBy { getActiveDexNumber(it, regionalFilter) }
    }

    val familyMembersMap = remember { PokemonDatabase.familyMembersByFamilyId }

    // 2. Filter base pool by Type, Era auto-sync, Rating Filter, Search Query, and Family Grouping
    val displayedEntries = remember(
        baseDexPool,
        regionalFilter,
        searchQuery,
        typeFilter,
        typeEra,
        ratingFilter,
        isEvolutionFamilyMode,
        preferredStages,
        ratings,
        tracker,
        pageMode,
        trackerFilterMode
    ) {
        val poolIds = baseDexPool.map { it.id }.toHashSet()

        val entries: List<Pair<Pokemon, List<Pokemon>>> = if (isEvolutionFamilyMode) {
            val seenFamilies = HashSet<String>()
            val grouped = ArrayList<Pair<Pokemon, List<Pokemon>>>()
            for (mon in baseDexPool) {
                if (seenFamilies.add(mon.familyId)) {
                    val allStages = familyMembersMap[mon.familyId] ?: listOf(mon)
                    val stagesInDex = allStages.filter { poolIds.contains(it.id) }.ifEmpty { allStages }
                    val prefId = preferredStages[mon.familyId]
                    val activeMon = when {
                        prefId != null -> stagesInDex.firstOrNull { it.id == prefId } ?: stagesInDex.last()
                        typeFilter != null -> stagesInDex.lastOrNull { stage ->
                            val (p, s) = stage.effectiveTypes(typeEra)
                            p == typeFilter || s == typeFilter
                        } ?: stagesInDex.last()
                        else -> stagesInDex.last()
                    }
                    grouped.add(activeMon to stagesInDex)
                }
            }
            grouped
        } else {
            baseDexPool.map { mon ->
                val stages = familyMembersMap[mon.familyId] ?: listOf(mon)
                mon to stages
            }
        }

        var filtered = entries

        // Filter by Element Type (using era-effective types!)
        if (typeFilter != null) {
            filtered = filtered.filter { (mon, stages) ->
                if (isEvolutionFamilyMode) {
                    stages.any { stage ->
                        val (p, s) = stage.effectiveTypes(typeEra)
                        p == typeFilter || s == typeFilter
                    }
                } else {
                    val (p, s) = mon.effectiveTypes(typeEra)
                    p == typeFilter || s == typeFilter
                }
            }
        }

        // Filter by Rating Option
        if (ratingFilter != RatingFilterOption.ALL) {
            filtered = filtered.filter { (mon, _) ->
                val r = ratings[mon.id]
                when (ratingFilter) {
                    RatingFilterOption.ALL -> true
                    RatingFilterOption.ALL_LIKED -> r?.status == RatingStatus.LIKE
                    RatingFilterOption.LIKED_ICON -> r?.status == RatingStatus.LIKE && r.tags.contains(RatingTag.ICON)
                    RatingFilterOption.LIKED_DESIGN -> r?.status == RatingStatus.LIKE && r.tags.contains(RatingTag.DESIGN)
                    RatingFilterOption.LIKED_PARTY -> r?.status == RatingStatus.LIKE && r.tags.contains(RatingTag.PARTY)
                    RatingFilterOption.DISLIKED_ONLY -> r?.status == RatingStatus.DISLIKE
                    RatingFilterOption.UNRATED_ONLY -> r == null || r.status == RatingStatus.UNRATED
                }
            }
        }

        // Filter in Catch 'Em All Tracker mode
        if (pageMode == PokedexPageMode.CATCH_EM_ALL_TRACKER && trackerFilterMode != TrackerFilterMode.ALL) {
            filtered = filtered.filter { (mon, _) ->
                val s = tracker[mon.id]?.status ?: TrackerStatus.UNSEEN
                when (trackerFilterMode) {
                    TrackerFilterMode.ALL -> true
                    TrackerFilterMode.CAUGHT -> s == TrackerStatus.CAUGHT
                    TrackerFilterMode.SEEN_ONLY -> s == TrackerStatus.SEEN
                    TrackerFilterMode.UNCAUGHT -> s != TrackerStatus.CAUGHT
                    TrackerFilterMode.UNSEEN -> s == TrackerStatus.UNSEEN
                }
            }
        }

        // Search Query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter { (mon, stages) ->
                val dexStr = getActiveDexNumber(mon, regionalFilter).toString()
                mon.name.lowercase().contains(q) ||
                        mon.id.toString() == q ||
                        dexStr == q ||
                        mon.formattedId().lowercase().contains(q) ||
                        (isEvolutionFamilyMode && stages.any { it.name.lowercase().contains(q) })
            }
        }

        filtered
    }

    // Stats for Love 'Em All Picker mode
    val likedCount = remember(ratings, baseDexPool) {
        baseDexPool.count { ratings[it.id]?.status == RatingStatus.LIKE }
    }
    val dislikeCount = remember(ratings, baseDexPool) {
        baseDexPool.count { ratings[it.id]?.status == RatingStatus.DISLIKE }
    }
    val totalRated = likedCount + dislikeCount
    val likedPct = if (totalRated > 0) (likedCount * 100) / totalRated else 0
    val dislikePct = if (totalRated > 0) 100 - likedPct else 0
    val likeFraction = if (totalRated > 0) likedCount.toFloat() / totalRated else 0.5f

    // Stats for Catch 'Em All Tracker mode
    val totalInDex = baseDexPool.size
    val caughtCount = remember(tracker, baseDexPool) {
        baseDexPool.count { (tracker[it.id]?.status ?: TrackerStatus.UNSEEN) == TrackerStatus.CAUGHT }
    }
    val seenTotalCount = remember(tracker, baseDexPool) {
        baseDexPool.count {
            val s = tracker[it.id]?.status ?: TrackerStatus.UNSEEN
            s == TrackerStatus.SEEN || s == TrackerStatus.CAUGHT
        }
    }
    val uncaughtCount = totalInDex - caughtCount
    val caughtPct = if (totalInDex > 0) (caughtCount * 100) / totalInDex else 0
    val seenPct = if (totalInDex > 0) (seenTotalCount * 100) / totalInDex else 0
    val uncaughtPct = if (totalInDex > 0) (uncaughtCount * 100) / totalInDex else 100

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkScaffoldBg)
    ) {
        // TOP CONTROL HEADER
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ROW 1: [DS] - [Picker | Tracker] - [✦ Shiny] - [↑ Family] - [History]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. [DS] Button with Display Settings Dropdown Menu
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceCard)
                                .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { showDsMenu = true }
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                                .testTag("pokedex_ds_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Display Settings",
                                tint = NeonBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "DS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDsMenu,
                            onDismissRequest = { showDsMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Display Settings",
                                color = NeonBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            HorizontalDivider(color = DarkCardBorder)
                            BadgeToggleMenuItem(
                                label = "BST (Base Stat Total)",
                                checked = badgeSettings.showBst,
                                onToggle = {
                                    viewModel.updateProfileBadgeSettings(
                                        badgeSettings.copy(showBst = !badgeSettings.showBst)
                                    )
                                }
                            )
                            BadgeToggleMenuItem(
                                label = "Tier Rating (S..F)",
                                checked = badgeSettings.showTierRating,
                                onToggle = {
                                    viewModel.updateProfileBadgeSettings(
                                        badgeSettings.copy(showTierRating = !badgeSettings.showTierRating)
                                    )
                                }
                            )
                            BadgeToggleMenuItem(
                                label = "Legendary & Mythical",
                                checked = badgeSettings.showLegendaryMythical,
                                onToggle = {
                                    viewModel.updateProfileBadgeSettings(
                                        badgeSettings.copy(showLegendaryMythical = !badgeSettings.showLegendaryMythical)
                                    )
                                }
                            )
                            BadgeToggleMenuItem(
                                label = "Noob Trap Warning",
                                checked = badgeSettings.showNoobTrap,
                                onToggle = {
                                    viewModel.updateProfileBadgeSettings(
                                        badgeSettings.copy(showNoobTrap = !badgeSettings.showNoobTrap)
                                    )
                                }
                            )
                            BadgeToggleMenuItem(
                                label = "Singular (∅ No Evo)",
                                checked = badgeSettings.showSingularNoEvo,
                                onToggle = {
                                    viewModel.updateProfileBadgeSettings(
                                        badgeSettings.copy(showSingularNoEvo = !badgeSettings.showSingularNoEvo)
                                    )
                                }
                            )
                        }
                    }

                    // 2. Mode Switcher: [Picker | Tracker]
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceCard)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        PokedexPageMode.entries.forEach { mode ->
                            val isSelected = pageMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) {
                                            if (mode == PokedexPageMode.LOVE_EM_ALL_PICKER) NeonAmber else Color(0xFF10B981)
                                        } else Color.Transparent
                                    )
                                    .clickable { viewModel.setPokedexPageMode(mode) }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (mode == PokedexPageMode.LOVE_EM_ALL_PICKER) "Picker" else "Tracker",
                                    color = if (isSelected) Color(0xFF0B0F19) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 3. [Tombol ✦ Shiny]
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isShiny) NeonAmber.copy(alpha = 0.25f) else Color(0xFF0F172A))
                            .border(
                                1.dp,
                                if (isShiny) NeonAmber else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.toggleShinyGlobal() }
                            .testTag("btn_shiny_global_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✦",
                            color = if (isShiny) NeonAmber else Color(0xFFCBD5E1),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 4. [Tombol ↑ Family]
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEvolutionFamilyMode) NeonBlue.copy(alpha = 0.25f) else Color(0xFF0F172A))
                            .border(
                                1.dp,
                                if (isEvolutionFamilyMode) NeonBlue else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.toggleSingleDexView() }
                            .testTag("btn_family_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↑",
                            color = if (isEvolutionFamilyMode) NeonBlue else Color(0xFFE2E8F0),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // 5. [Tombol History]
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.openRecentModal(true) }
                            .testTag("btn_history_modal"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Rating History",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // ROW 2: [Dropdown Pokédex] - [Dropdown Gens] - [Dropdown Types] - [Rating Filter (❖)]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. [Dropdown Pokédex]
                    Box(modifier = Modifier.weight(1.2f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, NeonBlue.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable { showDexMenu = true }
                                .padding(horizontal = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${regionalFilter.label} (${regionalFilter.count})",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Dex",
                                tint = NeonBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDexMenu,
                            onDismissRequest = { showDexMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            RegionalFilter.entries.forEach { rf ->
                                val isSel = regionalFilter == rf
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rf.label,
                                                color = if (isSel) NeonBlue else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "${rf.count}",
                                                color = if (isSel) NeonAmber else Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setRegionalFilter(rf)
                                        showDexMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. [Dropdown Gens] (Separate Generation Filter with Auto-Sync of 22 Pre-Fairy Pokémon)
                    Box(modifier = Modifier.weight(1.1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, NeonAmber.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable { showGensMenu = true }
                                .padding(horizontal = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = typeEra.shortLabel,
                                color = NeonAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Generation",
                                tint = NeonAmber,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showGensMenu,
                            onDismissRequest = { showGensMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "FILTER BY GENERATION",
                                color = NeonAmber,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            TypeEra.entries.filter { it != TypeEra.ALL_TYPES }.forEach { era ->
                                val isSelEra = typeEra == era
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = era.label,
                                            color = if (isSelEra) NeonAmber else Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelEra) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        viewModel.setTypeEra(era)
                                        showGensMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. [Dropdown Types] (Separate Element Type Selector)
                    Box(modifier = Modifier.weight(1.1f)) {
                        val activeType = typeFilter
                        val typeDisplayLabel = activeType?.displayName ?: "All Types"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeType?.color?.copy(alpha = 0.25f) ?: Color(0xFF0F172A))
                                .border(
                                    1.dp,
                                    activeType?.color ?: DarkCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { showTypesMenu = true }
                                .padding(horizontal = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = typeDisplayLabel,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Type",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showTypesMenu,
                            onDismissRequest = { showTypesMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "FILTER BY ELEMENT",
                                color = NeonBlue,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "All Elements (Reset)",
                                        color = if (typeFilter == null) NeonBlue else Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    viewModel.setTypeFilter(null)
                                    showTypesMenu = false
                                }
                            )

                            // Available types filtered by active generation era
                            val availableTypes = when (typeEra) {
                                TypeEra.GEN_1_15 -> PokemonType.entries.filter { it != PokemonType.DARK && it != PokemonType.STEEL && it != PokemonType.FAIRY }
                                TypeEra.GEN_2_5_17 -> PokemonType.entries.filter { it != PokemonType.FAIRY }
                                else -> PokemonType.entries
                            }

                            availableTypes.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TypePill(t)
                                            Text(t.displayName, color = Color.White, fontSize = 11.5.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setTypeFilter(t)
                                        showTypesMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. [Rating Filter ❖] (Only shown when Picker mode is active)
                    if (pageMode == PokedexPageMode.LOVE_EM_ALL_PICKER) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (ratingFilter != RatingFilterOption.ALL) NeonAmber.copy(alpha = 0.25f) else Color(0xFF0F172A)
                                    )
                                    .border(
                                        1.dp,
                                        if (ratingFilter != RatingFilterOption.ALL) NeonAmber else DarkCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showRatingBatchMenu = true }
                                    .testTag("btn_rating_batch_menu"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "❖",
                                    color = if (ratingFilter != RatingFilterOption.ALL) NeonAmber else Color(0xFFE2E8F0),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = showRatingBatchMenu,
                                onDismissRequest = { showRatingBatchMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = "RATING FILTER",
                                    color = NeonAmber,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                                RatingFilterOption.entries.forEach { opt ->
                                    val isSel = ratingFilter == opt
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = opt.label,
                                                color = if (isSel) NeonAmber else Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            viewModel.setRatingFilter(opt)
                                            showRatingBatchMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ROW 3: MODE DASHBOARD (Compact Like/Dislike Ratio Capsule OR Tracker 3 Summary Cards)
                if (pageMode == PokedexPageMode.LOVE_EM_ALL_PICKER) {
                    // COMPACT LIKE/DISLIKE RATIO CAPSULE (from /public/assets/aistudio/maunya_kegini_2ab7cd40.png)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0D1424))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left: ThumbUp Coral Pink + Liked Pct
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Liked",
                                    tint = Color(0xFFFF5E7E),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "$likedPct%",
                                    color = Color(0xFFFF7B93),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Center: Mini Dual-Color Capsule Bar
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF334155))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = likeFraction.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF5E7E))
                                )
                            }

                            // Right: ThumbDown Slate-Blue + Disliked Pct
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ThumbDown,
                                    contentDescription = "Disliked",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "$dislikePct%",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                } else {
                    // CATCH 'EM ALL TRACKER SUMMARY CARDS (Caught, Seen, Uncaught)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UnifiedTrackerSummaryCard(
                            title = "CAUGHT",
                            countText = "$caughtCount / $totalInDex",
                            percentText = "$caughtPct%",
                            progress = if (totalInDex > 0) caughtCount.toFloat() / totalInDex else 0f,
                            accentColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        UnifiedTrackerSummaryCard(
                            title = "SEEN",
                            countText = "$seenTotalCount / $totalInDex",
                            percentText = "$seenPct%",
                            progress = if (totalInDex > 0) seenTotalCount.toFloat() / totalInDex else 0f,
                            accentColor = Color(0xFF38BDF8),
                            modifier = Modifier.weight(1f)
                        )
                        UnifiedTrackerSummaryCard(
                            title = "UNCAUGHT",
                            countText = "$uncaughtCount / $totalInDex",
                            percentText = "$uncaughtPct%",
                            progress = if (totalInDex > 0) uncaughtCount.toFloat() / totalInDex else 1f,
                            accentColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Tracker Status Filter Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackerFilterMode.entries.forEach { mode ->
                            val isSel = trackerFilterMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonBlue else DarkSurfaceCard)
                                    .border(1.dp, if (isSel) NeonBlue else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { trackerFilterMode = mode }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = mode.label,
                                    color = if (isSel) Color(0xFF0B0F19) else Color(0xFFE2E8F0),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ROW 4: 44dp VERTICALLY CENTERED SINGLE-LINE SEARCH BAR + COMPACT COUNT + [1|2|3|4|5]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search name or #id...",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(NeonBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = if (isEvolutionFamilyMode) "${displayedEntries.size} Fam" else "${displayedEntries.size} Mon",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )

                        // 1 to 5 Columns Segmented Control
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceCard)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                                .padding(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            (1..5).forEach { col ->
                                val isSel = gridColumns == col
                                Box(
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) NeonBlue else Color.Transparent)
                                        .clickable { gridColumns = col },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$col",
                                        color = if (isSel) Color(0xFF0B0F19) else Color(0xFF64748B),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // MAIN POKÉDEX GRID WITH PINCH-TO-ZOOM GESTURE (1 to 5 columns per row)
        val gridSpacing = when {
            gridColumns >= 4 -> 6.dp
            gridColumns == 3 -> 8.dp
            else -> 10.dp
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        zoomScale *= zoom
                        if (zoomScale > 1.25f) {
                            if (gridColumns > 1) {
                                gridColumns--
                            }
                            zoomScale = 1.0f
                        } else if (zoomScale < 0.75f) {
                            if (gridColumns < 5) {
                                gridColumns++
                            }
                            zoomScale = 1.0f
                        }
                    }
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (gridColumns >= 4) 8.dp else 10.dp)
                    .testTag("pokemon_grid"),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing)
            ) {
                items(
                    items = displayedEntries,
                    key = { (mon, _) ->
                        if (isEvolutionFamilyMode) "fam_${mon.familyId}" else "mon_${mon.id}"
                    }
                ) { (pokemon, familyStages) ->
                    val rating = ratings[pokemon.id]
                    val dexNumberStr = formatActiveDexNumber(pokemon, regionalFilter)
                    val stagesForCard = if (isEvolutionFamilyMode && familyStages.size > 1) familyStages else emptyList()
                    val isMonShiny = isShiny || preferredShinyIds.contains(pokemon.id)

                    if (pageMode == PokedexPageMode.LOVE_EM_ALL_PICKER) {
                        PokemonCard(
                            pokemon = pokemon,
                            rating = rating,
                            isPartyMember = partyIds.contains(pokemon.id),
                            isShiny = isMonShiny,
                            displayDexNumber = dexNumberStr,
                            badgeSettings = badgeSettings,
                            isFamilyMode = isEvolutionFamilyMode,
                            familyStages = stagesForCard,
                            typeEra = typeEra,
                            gridColumns = gridColumns,
                            onSelectStage = { stageMon ->
                                viewModel.setPreferredStage(stageMon.familyId, stageMon.id)
                            },
                            onToggleTag = { tag ->
                                viewModel.toggleRatingTag(pokemon.id, tag)
                            },
                            onDislike = {
                                viewModel.markDislike(pokemon.id)
                            },
                            onUndo = {
                                viewModel.resetRating(pokemon.id)
                            },
                            onOpenDetail = {
                                viewModel.openDetailModal(pokemon)
                            },
                            onSetTier = { tier ->
                                viewModel.setCustomTier(pokemon.id, tier)
                            }
                        )
                    } else {
                        val status = tracker[pokemon.id]?.status ?: TrackerStatus.UNSEEN
                        UnifiedTrackerPokemonCard(
                            pokemon = pokemon,
                            rating = rating,
                            status = status,
                            displayDexNumber = dexNumberStr,
                            isShiny = isMonShiny,
                            badgeSettings = badgeSettings,
                            isFamilyMode = isEvolutionFamilyMode,
                            familyStages = stagesForCard,
                            typeEra = typeEra,
                            gridColumns = gridColumns,
                            onSelectStage = { stageMon ->
                                viewModel.setPreferredStage(stageMon.familyId, stageMon.id)
                            },
                            onSetStatus = { newStatus ->
                                viewModel.setTrackerStatus(pokemon.id, newStatus)
                            },
                            onSetTier = { tier ->
                                viewModel.setCustomTier(pokemon.id, tier)
                            },
                            onOpenDetail = {
                                viewModel.openDetailModal(pokemon)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeToggleMenuItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (checked) NeonBlue else Color(0xFF1E293B))
                        .border(1.dp, if (checked) NeonBlue else Color(0xFF475569), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF0B0F19),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        },
        onClick = onToggle
    )
}

@Composable
private fun UnifiedTrackerPokemonCard(
    pokemon: Pokemon,
    rating: PokemonRating?,
    status: TrackerStatus,
    displayDexNumber: String,
    isShiny: Boolean,
    badgeSettings: ProfileBadgeSettings,
    isFamilyMode: Boolean,
    familyStages: List<Pokemon>,
    typeEra: TypeEra,
    gridColumns: Int,
    onSelectStage: (Pokemon) -> Unit,
    onSetStatus: (TrackerStatus) -> Unit,
    onSetTier: (Tier?) -> Unit,
    onOpenDetail: () -> Unit
) {
    var showTierMenu by remember { mutableStateOf(false) }
    val isUnseen = status == TrackerStatus.UNSEEN
    val activeTier = rating?.customTier ?: pokemon.defaultTier
    val tierSingleLetter = if (activeTier == Tier.TRAP) "F" else activeTier.label
    val (effPrim, effSec) = pokemon.effectiveTypes(typeEra)

    val solidTierBg = when (activeTier) {
        Tier.S -> Color(0xFF7C3AED)
        Tier.A -> Color(0xFFEA580C)
        Tier.B -> Color(0xFFD97706)
        Tier.C -> Color(0xFF059669)
        Tier.D -> Color(0xFF2563EB)
        Tier.F -> Color(0xFF475569)
        Tier.TRAP -> Color(0xFFDC2626)
    }

    val cardBorder = when (status) {
        TrackerStatus.CAUGHT -> Color(0xFF10B981).copy(alpha = 0.7f)
        TrackerStatus.SEEN -> Color(0xFF38BDF8).copy(alpha = 0.6f)
        TrackerStatus.UNSEEN -> DarkCardBorder
    }

    val isMicroGrid = gridColumns >= 4
    val isThreeCol = gridColumns == 3
    val isCompactButtons = gridColumns >= 3

    val shortDexStr = remember(displayDexNumber, gridColumns) {
        if (gridColumns >= 3 && displayDexNumber.startsWith("#0") && displayDexNumber.length == 5) {
            "#" + displayDexNumber.substring(2)
        } else {
            displayDexNumber
        }
    }

    val spriteBoxHeight = when (gridColumns) {
        1 -> 136.dp
        2 -> 118.dp
        3 -> 80.dp
        4 -> 56.dp
        else -> 44.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (isMicroGrid) 10.dp else 12.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(if (isMicroGrid) 10.dp else 12.dp))
            .testTag("tracker_card_${pokemon.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isMicroGrid) 5.dp else 8.dp, vertical = if (isMicroGrid) 5.dp else 8.dp)
        ) {
            // Top Row: Dex No + BST + Solid Single-letter Tier (No collision!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isThreeCol || isMicroGrid) 3.dp else 6.dp)
                ) {
                    when {
                        isMicroGrid -> {
                            Text(
                                text = shortDexStr,
                                color = Color(0xFFCBD5E1),
                                fontSize = if (gridColumns >= 5) 7.5.sp else 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        isThreeCol -> {
                            val headerText = if (badgeSettings.showBst) "$shortDexStr • ${pokemon.bst}" else shortDexStr
                            Text(
                                text = headerText,
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            Text(
                                text = displayDexNumber,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (badgeSettings.showBst) {
                                Text(
                                    text = "BST: ${pokemon.bst}",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (badgeSettings.showTierRating) {
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(if (isMicroGrid) 4.dp else 5.dp))
                                .background(solidTierBg)
                                .clickable { showTierMenu = true }
                                .padding(horizontal = if (isMicroGrid) 4.dp else 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = tierSingleLetter,
                                color = Color.White,
                                fontSize = if (isMicroGrid) 7.5.sp else if (isThreeCol) 9.sp else 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        DropdownMenu(
                            expanded = showTierMenu,
                            onDismissRequest = { showTierMenu = false }
                        ) {
                            Tier.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.label}-Tier") },
                                    onClick = {
                                        onSetTier(t)
                                        showTierMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Reset to Default") },
                                onClick = {
                                    onSetTier(null)
                                    showTierMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Sprite (clickable for detail)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spriteBoxHeight)
                    .clickable { onOpenDetail() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = pokemon.spriteUrl(isShiny),
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(spriteBoxHeight - 2.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isUnseen) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                )
            }

            // Name + Type (Strict single-line anti-wrap guard)
            Text(
                text = pokemon.name,
                color = if (isUnseen) Color(0xFF94A3B8) else Color.White,
                fontSize = when (gridColumns) {
                    1 -> 14.sp
                    2 -> 13.sp
                    3 -> 11.sp
                    4 -> 9.5.sp
                    else -> 8.5.sp
                },
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.padding(top = 2.dp, bottom = if (isMicroGrid) 4.dp else 6.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isMicroGrid || isThreeCol) 3.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypePill(
                    type = effPrim,
                    useShortCode = isMicroGrid,
                    compact = isThreeCol || isMicroGrid
                )
                effSec?.let {
                    TypePill(
                        type = it,
                        useShortCode = isMicroGrid,
                        compact = isThreeCol || isMicroGrid
                    )
                }
            }

            // Evolution stages row ONLY if Family Mode is active
            if (isFamilyMode && familyStages.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    familyStages.take(4).forEach { stage ->
                        val isCur = stage.id == pokemon.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCur) Color(0xFF1E293B) else Color(0xFF0F172A))
                                .border(0.8.dp, if (isCur) NeonBlue else DarkCardBorder, RoundedCornerShape(4.dp))
                                .clickable { onSelectStage(stage) }
                                .padding(vertical = 2.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stage.name,
                                color = if (isCur) NeonBlue else Color(0xFF94A3B8),
                                fontSize = if (isMicroGrid) 6.5.sp else 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3-way status toggles: [Belum | Seen | Caught] in 1-2 cols, [– | 👁 | ✓] in 3-5 cols
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                    .padding(if (isMicroGrid) 1.dp else 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TrackerStatusButton(
                    label = if (isCompactButtons) "–" else "Belum",
                    isSelected = status == TrackerStatus.UNSEEN,
                    activeColor = Color(0xFF64748B),
                    compact = isCompactButtons,
                    onClick = { onSetStatus(TrackerStatus.UNSEEN) },
                    modifier = Modifier.weight(1f)
                )
                TrackerStatusButton(
                    label = if (isCompactButtons) "👁" else "Seen",
                    isSelected = status == TrackerStatus.SEEN,
                    activeColor = Color(0xFF38BDF8),
                    compact = isCompactButtons,
                    onClick = { onSetStatus(TrackerStatus.SEEN) },
                    modifier = Modifier.weight(1f)
                )
                TrackerStatusButton(
                    label = if (isCompactButtons) "✓" else "Caught",
                    isSelected = status == TrackerStatus.CAUGHT,
                    activeColor = Color(0xFF10B981),
                    compact = isCompactButtons,
                    onClick = { onSetStatus(TrackerStatus.CAUGHT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrackerStatusButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    compact: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = if (compact) 3.dp else 4.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF0B0F19) else Color(0xFF94A3B8),
            fontSize = if (compact) 8.5.sp else 9.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun UnifiedTrackerSummaryCard(
    title: String,
    countText: String,
    percentText: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = percentText, color = accentColor, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = countText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = accentColor,
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}
