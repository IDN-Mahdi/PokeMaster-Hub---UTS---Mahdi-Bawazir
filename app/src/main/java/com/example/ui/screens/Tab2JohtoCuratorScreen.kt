package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.PokemonDatabase
import com.example.ui.JohtoSubTab
import com.example.ui.PokemonViewModel
import com.example.ui.components.TypePill
import com.example.ui.theme.*

enum class StatSort(val label: String) {
    DEX("Dex #"),
    BST("BST"),
    HP("HP"),
    ATK("Atk"),
    DEF("Def"),
    SPA("SpA"),
    SPD("SpD"),
    SPE("Spe")
}

enum class TrackerFilterMode(val label: String) {
    ALL("All"),
    CAUGHT("Caught"),
    SEEN_ONLY("Seen Only"),
    UNCAUGHT("Uncaught"),
    UNSEEN("Unseen")
}

@Composable
fun Tab2JohtoCuratorScreen(
    viewModel: PokemonViewModel,
    modifier: Modifier = Modifier
) {
    val subTab by viewModel.johtoSubTab.collectAsState()
    val isModern by viewModel.isModern.collectAsState()
    val isShiny by viewModel.isShinyGlobal.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val tracker by viewModel.tracker.collectAsState()

    val primaryTabs = remember {
        listOf(
            JohtoSubTab.FAMILIES,
            JohtoSubTab.TYPE_SPECIALISTS,
            JohtoSubTab.TRACKER,
            JohtoSubTab.FREE_RANKER
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkScaffoldBg)
    ) {
        // Clean Sub-Tab Navigation Bar
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                primaryTabs.forEach { tab ->
                    val isSelected = subTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonBlue else DarkSurfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) NeonBlue else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setJohtoSubTab(tab) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color(0xFF0B0F19) else Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        when (subTab) {
            JohtoSubTab.FAMILIES -> {
                JohtoFamiliesView(
                    viewModel = viewModel,
                    ratings = ratings,
                    tracker = tracker,
                    isShiny = isShiny,
                    isModern = isModern
                )
            }
            JohtoSubTab.TYPE_SPECIALISTS -> {
                JohtoModeATypeSpecialistsView(
                    viewModel = viewModel,
                    isModern = isModern,
                    isShiny = isShiny
                )
            }
            JohtoSubTab.TRACKER -> {
                JohtoPokedexTrackerView(
                    viewModel = viewModel,
                    isShiny = isShiny
                )
            }
            JohtoSubTab.FREE_RANKER -> {
                JohtoModeBFreeRankerView(
                    viewModel = viewModel,
                    isShiny = isShiny
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SUB-TAB 1: 129 EVOLUTION FAMILIES (MATCHING REFERENCE IMAGE 1)
// ---------------------------------------------------------------------------
@Composable
fun JohtoFamiliesView(
    viewModel: PokemonViewModel,
    ratings: Map<Int, PokemonRating>,
    tracker: Map<Int, PokemonTrackerEntry>,
    isShiny: Boolean,
    isModern: Boolean
) {
    var statSort by remember { mutableStateOf(StatSort.DEX) }
    var selectedTypeFilter by remember { mutableStateOf<PokemonType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSeparated256 by remember { mutableStateOf(false) }
    val preferredStages by viewModel.preferredStages.collectAsState()

    val canonicalFamilies = remember { PokemonDatabase.johtoFamiliesList }
    val johto256 = remember { PokemonDatabase.johtoDexPokemon }

    // Build displayed family entries: Pair(ActivePokemon, FamilyStagesList)
    val displayedEntries = remember(
        canonicalFamilies,
        johto256,
        isSeparated256,
        selectedTypeFilter,
        preferredStages,
        statSort,
        searchQuery
    ) {
        val rawEntries: List<Pair<Pokemon, List<Pokemon>>> = if (isSeparated256) {
            johto256.map { mon -> Pair(mon, listOf(mon)) }
        } else {
            canonicalFamilies.mapNotNull { familyMons ->
                if (familyMons.isEmpty()) return@mapNotNull null
                val famKey = familyMons.first().familyId
                val activeMon = if (selectedTypeFilter != null) {
                    familyMons.lastOrNull {
                        it.primaryType == selectedTypeFilter || it.secondaryType == selectedTypeFilter
                    } ?: return@mapNotNull null
                } else {
                    val prefId = preferredStages[famKey]
                    if (prefId != null) {
                        familyMons.firstOrNull { it.id == prefId } ?: familyMons.last()
                    } else {
                        familyMons.last()
                    }
                }
                Pair(activeMon, familyMons)
            }
        }

        // Filter by type if separated mode
        val typeFiltered = if (isSeparated256 && selectedTypeFilter != null) {
            rawEntries.filter { (mon, _) ->
                mon.primaryType == selectedTypeFilter || mon.secondaryType == selectedTypeFilter
            }
        } else {
            rawEntries
        }

        // Filter by search query
        val searchFiltered = if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            typeFiltered.filter { (mon, stages) ->
                mon.name.lowercase().contains(q) ||
                        mon.id.toString() == q ||
                        (mon.johtoDexNo?.toString() == q) ||
                        stages.any { it.name.lowercase().contains(q) }
            }
        } else {
            typeFiltered
        }

        // Sort
        when (statSort) {
            StatSort.DEX -> searchFiltered.sortedBy { it.first.johtoDexNo ?: it.first.id }
            StatSort.BST -> searchFiltered.sortedByDescending { it.first.bst }
            StatSort.HP -> searchFiltered.sortedByDescending { it.first.hp }
            StatSort.ATK -> searchFiltered.sortedByDescending { it.first.attack }
            StatSort.DEF -> searchFiltered.sortedByDescending { it.first.defense }
            StatSort.SPA -> searchFiltered.sortedByDescending { it.first.spAtk }
            StatSort.SPD -> searchFiltered.sortedByDescending { it.first.spDef }
            StatSort.SPE -> searchFiltered.sortedByDescending { it.first.speed }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Control Header Bar
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                // Search + 129 Families / 256 Separated Toggle + Sort Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search name or #...", fontSize = 11.sp, color = Color(0xFF64748B))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White)
                    )

                    // 129 Families vs 256 Separated Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceCard)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isSeparated256) NeonBlue else Color.Transparent)
                                .clickable { isSeparated256 = false }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "129 Families",
                                color = if (!isSeparated256) Color(0xFF0B0F19) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSeparated256) NeonBlue else Color.Transparent)
                                .clickable { isSeparated256 = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "256 All",
                                color = if (isSeparated256) Color(0xFF0B0F19) else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Stat Sort Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("SORT:", color = Color(0xFF64748B), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                    StatSort.entries.forEach { s ->
                        val isSel = statSort == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) NeonAmber else DarkSurfaceCard)
                                .border(1.dp, if (isSel) NeonAmber else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { statSort = s }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = s.label,
                                color = if (isSel) Color(0xFF0B0F19) else Color(0xFFCBD5E1),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Adaptive Type Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedTypeFilter == null) Color.White else DarkSurfaceCard)
                            .border(1.dp, if (selectedTypeFilter == null) Color.White else DarkCardBorder, RoundedCornerShape(6.dp))
                            .clickable { selectedTypeFilter = null }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "All (${displayedEntries.size})",
                            color = if (selectedTypeFilter == null) Color(0xFF0B0F19) else Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    PokemonType.activeTypes(isModern).forEach { t ->
                        val isSel = selectedTypeFilter == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) t.color else DarkSurfaceCard)
                                .border(1.dp, t.color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable { selectedTypeFilter = if (isSel) null else t }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(t.displayName, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Grid of Clean Symmetric Johto Family Cards (Matching image_acd144.png)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 162.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = displayedEntries,
                key = { (mon, stages) -> if (isSeparated256) "sep_${mon.id}" else "fam_${stages.first().id}" }
            ) { (displayedMon, familyStages) ->
                val rating = ratings[displayedMon.id]
                val trackerStatus = tracker[displayedMon.id]?.status ?: TrackerStatus.UNSEEN

                JohtoCleanFamilyCard(
                    pokemon = displayedMon,
                    familyStages = familyStages,
                    rating = rating,
                    trackerStatus = trackerStatus,
                    isShiny = isShiny,
                    onToggleSeen = {
                        val next = if (trackerStatus == TrackerStatus.SEEN || trackerStatus == TrackerStatus.CAUGHT) {
                            TrackerStatus.UNSEEN
                        } else {
                            TrackerStatus.SEEN
                        }
                        viewModel.setTrackerStatus(displayedMon.id, next)
                    },
                    onToggleCaught = {
                        val next = if (trackerStatus == TrackerStatus.CAUGHT) {
                            TrackerStatus.SEEN
                        } else {
                            TrackerStatus.CAUGHT
                        }
                        viewModel.setTrackerStatus(displayedMon.id, next)
                    },
                    onToggleUnwanted = {
                        val currentStatus = rating?.status ?: RatingStatus.UNRATED
                        if (currentStatus == RatingStatus.DISLIKE) {
                            viewModel.resetRating(displayedMon.id)
                        } else {
                            viewModel.ratePokemon(displayedMon.id, RatingStatus.DISLIKE)
                        }
                    },
                    onToggleFavorite = {
                        val currentStatus = rating?.status ?: RatingStatus.UNRATED
                        if (currentStatus == RatingStatus.LIKE) {
                            viewModel.resetRating(displayedMon.id)
                        } else {
                            viewModel.ratePokemon(displayedMon.id, RatingStatus.LIKE)
                        }
                    },
                    onSelectStage = { stageMon ->
                        viewModel.setPreferredStage(stageMon.familyId, stageMon.id)
                    },
                    onOpenDetail = {
                        viewModel.openDetailModal(displayedMon)
                    }
                )
            }
        }
    }
}

@Composable
fun JohtoCleanFamilyCard(
    pokemon: Pokemon,
    familyStages: List<Pokemon>,
    rating: PokemonRating?,
    trackerStatus: TrackerStatus,
    isShiny: Boolean,
    onToggleSeen: () -> Unit,
    onToggleCaught: () -> Unit,
    onToggleUnwanted: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSelectStage: (Pokemon) -> Unit,
    onOpenDetail: () -> Unit
) {
    val isLiked = rating?.status == RatingStatus.LIKE
    val isDisliked = rating?.status == RatingStatus.DISLIKE
    val isSeen = trackerStatus == TrackerStatus.SEEN || trackerStatus == TrackerStatus.CAUGHT
    val isCaught = trackerStatus == TrackerStatus.CAUGHT

    val cardBorderColor = when {
        isLiked -> TagGoldBorder.copy(alpha = 0.7f)
        isDisliked -> Color(0xFF7F1D1D)
        isCaught -> Color(0xFF10B981).copy(alpha = 0.5f)
        else -> DarkCardBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .testTag("johto_family_card_${pokemon.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // 1. Top Row: Dex Number on Left + 4 Micro Status Icons on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pokemon.johtoDexNo?.let { "#${it.toString().padStart(3, '0')}" }
                        ?: pokemon.formattedShortId(),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Micro Icon 1: Seen (Eye)
                    MicroStatusIconButton(
                        active = isSeen,
                        activeColor = NeonBlue,
                        contentDescription = "Seen",
                        onClick = onToggleSeen
                    ) { tint ->
                        Icon(
                            imageVector = if (isSeen) Icons.Filled.Visibility else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Micro Icon 2: Caught (Geometric Pokéball Vector)
                    MicroStatusIconButton(
                        active = isCaught,
                        activeColor = NeonEmerald,
                        contentDescription = "Caught",
                        onClick = onToggleCaught
                    ) { tint ->
                        PokeballMicroVectorIcon(tint = tint, filled = isCaught)
                    }

                    // Micro Icon 3: Unwanted (Block)
                    MicroStatusIconButton(
                        active = isDisliked,
                        activeColor = Color(0xFFEF4444),
                        contentDescription = "Unwanted",
                        onClick = onToggleUnwanted
                    ) { tint ->
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // Micro Icon 4: Favorite (Star)
                    MicroStatusIconButton(
                        active = isLiked,
                        activeColor = NeonAmber,
                        contentDescription = "Favorite",
                        onClick = onToggleFavorite
                    ) { tint ->
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Center Clean Dark Box with Sprite (No overlapping 'i' or 'undo' buttons)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B0F19))
                    .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(10.dp))
                    .clickable { onOpenDetail() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = pokemon.spriteUrl(isShiny),
                    contentDescription = pokemon.name,
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Name + Type Badges + BST Badge
            Text(
                text = pokemon.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onOpenDetail() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypePill(pokemon.primaryType)
                    pokemon.secondaryType?.let { TypePill(it) }
                }

                Text(
                    text = "BST ${pokemon.bst}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Bottom Evolution Stage Selector Pills ([Chikorita] | [Bayleef] | [Meganium])
            if (familyStages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    familyStages.forEach { stage ->
                        val isCurrentStage = stage.id == pokemon.id
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isCurrentStage) Color(0xFF1E293B) else Color(0xFF0B0F19)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrentStage) NeonBlue.copy(alpha = 0.6f) else DarkCardBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectStage(stage) }
                                .padding(top = 4.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stage.name,
                                color = if (isCurrentStage) Color.White else Color(0xFF64748B),
                                fontSize = 8.5.sp,
                                fontWeight = if (isCurrentStage) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(2.dp)
                                    .background(
                                        if (isCurrentStage) NeonBlue else Color.Transparent,
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicroStatusIconButton(
    active: Boolean,
    activeColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    iconContent: @Composable (Color) -> Unit
) {
    val tint = if (active) activeColor else Color(0xFF475569)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (active) activeColor.copy(alpha = 0.16f) else Color(0xFF0B0F19))
            .border(
                1.dp,
                if (active) activeColor.copy(alpha = 0.6f) else DarkCardBorder,
                RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        iconContent(tint)
    }
}

@Composable
private fun PokeballMicroVectorIcon(tint: Color, filled: Boolean) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = tint,
            radius = r,
            style = Stroke(width = 1.4.dp.toPx())
        )
        drawLine(
            color = tint,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.2.dp.toPx()
        )
        drawCircle(
            color = if (filled) tint else Color(0xFF0B0F19),
            radius = r * 0.35f,
            center = center
        )
        drawCircle(
            color = tint,
            radius = r * 0.35f,
            center = center,
            style = Stroke(width = 1.1.dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------
// SUB-TAB 2: TYPE SPECIALISTS BOARD (MATCHING REFERENCE IMAGE 2)
// ---------------------------------------------------------------------------
@Composable
fun JohtoModeATypeSpecialistsView(
    viewModel: PokemonViewModel,
    isModern: Boolean,
    isShiny: Boolean
) {
    val activeTypes = remember(isModern) { PokemonType.activeTypes(isModern) }
    val assignments by viewModel.typeAssignments.collectAsState()
    val quotas by viewModel.typeQuotas.collectAsState()
    val pokeMap = viewModel.pokeMap

    var quickSetTopN by remember { mutableStateOf(3) }
    var filterType by remember { mutableStateOf<PokemonType?>(null) }
    var assignModalType by remember { mutableStateOf<PokemonType?>(null) }

    val totalFilled = remember(assignments, activeTypes) {
        activeTypes.sumOf { t -> (assignments[t]?.size ?: 0) }
    }
    val totalCapacity = remember(quotas, activeTypes) {
        activeTypes.sumOf { t -> (quotas[t] ?: 3) }
    }
    val completeTypesCount = remember(assignments, quotas, activeTypes) {
        activeTypes.count { t ->
            val q = quotas[t] ?: 3
            val c = assignments[t]?.size ?: 0
            c >= q && q > 0
        }
    }

    val displayedTypes = remember(activeTypes, filterType) {
        if (filterType != null) listOf(filterType!!) else activeTypes
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Summary & Quick Set All Dashboard (Matching image_acd17f.png)
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Box 1: TOTAL FILLED
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceCard)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TOTAL FILLED", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = "$totalFilled / $totalCapacity",
                                color = NeonBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Icon(Icons.Default.Groups, contentDescription = null, tint = NeonBlue.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }

                    // Box 2: COMPLETE TYPES
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceCard)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("COMPLETE TYPES", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = "$completeTypesCount / ${activeTypes.size}",
                                color = NeonEmerald,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Icon(Icons.Default.Verified, contentDescription = null, tint = NeonEmerald.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }

                // Quick Set All 17/18 Types Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Quick Set All ${activeTypes.size}:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        listOf(3, 5, 10).forEach { n ->
                            val isSel = quickSetTopN == n
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isSel) NeonBlue.copy(alpha = 0.2f) else Color(0xFF0B0F19))
                                    .border(1.dp, if (isSel) NeonBlue else DarkCardBorder, RoundedCornerShape(5.dp))
                                    .clickable { quickSetTopN = n }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Top $n",
                                    color = if (isSel) NeonBlue else Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonBlue)
                                .clickable { viewModel.applyQuickSetAllTypes(quickSetTopN) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Apply to All", color = Color(0xFF0B0F19), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable { viewModel.clearAllTypeSpecialists() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Clear All", color = Color(0xFFFCA5A5), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Type Filter Scroller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (filterType == null) Color.White else DarkSurfaceCard)
                            .border(1.dp, if (filterType == null) Color.White else DarkCardBorder, RoundedCornerShape(6.dp))
                            .clickable { filterType = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "All ${activeTypes.size} Boards",
                            color = if (filterType == null) Color(0xFF0B0F19) else Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    activeTypes.forEach { t ->
                        val isSel = filterType == t
                        val count = assignments[t]?.size ?: 0
                        val q = quotas[t] ?: 3
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) t.color else DarkSurfaceCard)
                                .border(1.dp, t.color, RoundedCornerShape(6.dp))
                                .clickable { filterType = if (isSel) null else t }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${t.displayName} ($count/$q)",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Scrollable List of Type Specialist Boards
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayedTypes, key = { it.name }) { type ->
                val quota = quotas[type] ?: 3
                val assignedIds = assignments[type] ?: emptyList()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, type.color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Board Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(type.color)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = type.displayName.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Text(
                                    text = "${assignedIds.size} / $quota Filled",
                                    color = if (assignedIds.size >= quota) NeonEmerald else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Per-Type Quota Selector (Top 1..10)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Quota:", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF0B0F19))
                                        .border(1.dp, DarkCardBorder, RoundedCornerShape(5.dp))
                                        .clickable { if (quota > 1) viewModel.setTypeQuota(type, quota - 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "Top $quota",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF0B0F19))
                                        .border(1.dp, DarkCardBorder, RoundedCornerShape(5.dp))
                                        .clickable { if (quota < 10) viewModel.setTypeQuota(type, quota + 1) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Slots 1..quota
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (slotIdx in 0 until quota) {
                                val monId = assignedIds.getOrNull(slotIdx)
                                val mon = monId?.let { pokeMap[it] }

                                if (mon != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0B0F19))
                                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Rank + Up/Down controls
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move Up",
                                                    tint = if (slotIdx > 0) Color(0xFF94A3B8) else Color(0xFF334155),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable(enabled = slotIdx > 0) {
                                                            viewModel.reorderTypeSpecialist(type, slotIdx, slotIdx - 1)
                                                        }
                                                )
                                                Text(
                                                    text = "#${slotIdx + 1}",
                                                    color = NeonAmber,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move Down",
                                                    tint = if (slotIdx < assignedIds.size - 1) Color(0xFF94A3B8) else Color(0xFF334155),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable(enabled = slotIdx < assignedIds.size - 1) {
                                                            viewModel.reorderTypeSpecialist(type, slotIdx, slotIdx + 1)
                                                        }
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            AsyncImage(
                                                model = mon.spriteUrl(isShiny),
                                                contentDescription = mon.name,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clickable { viewModel.openDetailModal(mon) }
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = mon.name,
                                                        color = Color.White,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = mon.johtoDexNo?.let { "#${it.toString().padStart(3, '0')}" }
                                                            ?: mon.formattedShortId(),
                                                        color = Color(0xFF64748B),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TypePill(mon.primaryType)
                                                    mon.secondaryType?.let { TypePill(it) }
                                                    Text(
                                                        text = "BST ${mon.bst}",
                                                        color = NeonBlue,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // Secondary Type Transfer Button ("To Flying", "To Dark") + Delete Button
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (mon.secondaryType != null) {
                                                val otherType = if (mon.primaryType == type) mon.secondaryType else mon.primaryType
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(otherType.color.copy(alpha = 0.2f))
                                                        .border(1.dp, otherType.color, RoundedCornerShape(6.dp))
                                                        .clickable { viewModel.moveToSecondaryType(type, mon.id) }
                                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "To ${otherType.displayName}",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeFromTypeSpecialist(type, mon.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Empty Slot Button: "+ Assign [Type] Pokémon"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0B0F19).copy(alpha = 0.6f))
                                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                            .clickable { assignModalType = type }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+ Assign ${type.displayName} Pokémon (Slot #${slotIdx + 1})",
                                            color = Color(0xFF64748B),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Assign Candidate Modal for Selected Type
    assignModalType?.let { targetType ->
        val assignedIds = assignments[targetType] ?: emptyList()
        val candidates = remember(targetType) {
            PokemonDatabase.johtoDexPokemon.filter {
                it.primaryType == targetType || it.secondaryType == targetType
            }.sortedByDescending { it.bst }
        }
        var modalSearch by remember { mutableStateOf("") }
        val filteredCandidates = remember(candidates, modalSearch) {
            if (modalSearch.isBlank()) candidates
            else candidates.filter { it.name.lowercase().contains(modalSearch.trim().lowercase()) }
        }

        Dialog(onDismissRequest = { assignModalType = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, targetType.color, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign ${targetType.displayName} Specialist",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = { assignModalType = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = modalSearch,
                        onValueChange = { modalSearch = it },
                        placeholder = { Text("Search ${targetType.displayName} Pokémon...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = targetType.color,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCandidates, key = { it.id }) { mon ->
                            val alreadyIn = assignedIds.contains(mon.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceCard)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !alreadyIn) {
                                        viewModel.assignToTypeSpecialist(targetType, mon.id)
                                        assignModalType = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = mon.spriteUrl(isShiny), contentDescription = null, modifier = Modifier.size(34.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(mon.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("BST ${mon.bst}", color = NeonBlue, fontSize = 10.sp)
                                    }
                                }
                                if (alreadyIn) {
                                    Text("Assigned", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("+ Select", color = targetType.color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SUB-TAB 3: 256 JOHTO POKÉDEX TRACKER (MATCHING REFERENCE IMAGE 3)
// ---------------------------------------------------------------------------
@Composable
fun JohtoPokedexTrackerView(
    viewModel: PokemonViewModel,
    isShiny: Boolean
) {
    val tracker by viewModel.tracker.collectAsState()
    val johtoPokemon = remember { PokemonDatabase.johtoDexPokemon }

    var filterMode by remember { mutableStateOf(TrackerFilterMode.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val total = johtoPokemon.size // 256
    val caughtCount = remember(tracker, johtoPokemon) {
        johtoPokemon.count { (tracker[it.id]?.status ?: TrackerStatus.UNSEEN) == TrackerStatus.CAUGHT }
    }
    val seenTotalCount = remember(tracker, johtoPokemon) {
        johtoPokemon.count {
            val s = tracker[it.id]?.status ?: TrackerStatus.UNSEEN
            s == TrackerStatus.SEEN || s == TrackerStatus.CAUGHT
        }
    }
    val uncaughtCount = total - caughtCount

    val caughtPct = if (total > 0) (caughtCount * 100) / total else 0
    val seenPct = if (total > 0) (seenTotalCount * 100) / total else 0
    val uncaughtPct = if (total > 0) (uncaughtCount * 100) / total else 100

    val filteredList = remember(johtoPokemon, tracker, filterMode, searchQuery) {
        var list = johtoPokemon.filter { mon ->
            val st = tracker[mon.id]?.status ?: TrackerStatus.UNSEEN
            when (filterMode) {
                TrackerFilterMode.ALL -> true
                TrackerFilterMode.CAUGHT -> st == TrackerStatus.CAUGHT
                TrackerFilterMode.SEEN_ONLY -> st == TrackerStatus.SEEN
                TrackerFilterMode.UNCAUGHT -> st != TrackerStatus.CAUGHT
                TrackerFilterMode.UNSEEN -> st == TrackerStatus.UNSEEN
            }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.id.toString() == q ||
                        (it.johtoDexNo?.toString() == q)
            }
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 3 Top Progress Cards: CAUGHT, SEEN, UNCAUGHT (Matching image_acd1da.jpg)
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrackerSummaryCard(
                        title = "CAUGHT",
                        countText = "$caughtCount / $total",
                        percentText = "$caughtPct%",
                        progress = if (total > 0) caughtCount.toFloat() / total else 0f,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    TrackerSummaryCard(
                        title = "SEEN",
                        countText = "$seenTotalCount / $total",
                        percentText = "$seenPct%",
                        progress = if (total > 0) seenTotalCount.toFloat() / total else 0f,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    TrackerSummaryCard(
                        title = "UNCAUGHT",
                        countText = "$uncaughtCount / $total",
                        percentText = "$uncaughtPct%",
                        progress = if (total > 0) uncaughtCount.toFloat() / total else 1f,
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Filter Pills + Search Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackerFilterMode.entries.forEach { mode ->
                        val isSel = filterMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonBlue else DarkSurfaceCard)
                                .border(1.dp, if (isSel) NeonBlue else DarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable { filterMode = mode }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSel) Color(0xFF0B0F19) else Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search 256 Johto Pokédex...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceCard,
                        unfocusedContainerColor = DarkSurfaceCard
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White)
                )
            }
        }

        // Grid of 256 Johto Pokédex Cards with [Belum] | [Seen] | [Caught] Buttons
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { mon ->
                val status = tracker[mon.id]?.status ?: TrackerStatus.UNSEEN
                val isUnseen = status == TrackerStatus.UNSEEN

                val cardBorder = when (status) {
                    TrackerStatus.CAUGHT -> Color(0xFF10B981).copy(alpha = 0.7f)
                    TrackerStatus.SEEN -> Color(0xFF38BDF8).copy(alpha = 0.6f)
                    TrackerStatus.UNSEEN -> DarkCardBorder
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Row: #001 + Status Dot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mon.johtoDexNo?.let { "#${it.toString().padStart(3, '0')}" }
                                    ?: mon.formattedShortId(),
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            TrackerStatus.CAUGHT -> Color(0xFF10B981)
                                            TrackerStatus.SEEN -> Color(0xFF38BDF8)
                                            TrackerStatus.UNSEEN -> Color(0xFF475569)
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Sprite Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0B0F19))
                                .clickable { viewModel.openDetailModal(mon) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUnseen) {
                                AsyncImage(
                                    model = mon.spriteUrl(isShiny),
                                    contentDescription = mon.name,
                                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply {
                                        setToScale(0.35f, 0.35f, 0.35f, 0.55f)
                                    }),
                                    modifier = Modifier.size(68.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = mon.spriteUrl(isShiny),
                                    contentDescription = mon.name,
                                    modifier = Modifier.size(68.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Real Pokémon Name (Always visible so user can track accurately)
                        Text(
                            text = mon.name,
                            color = if (isUnseen) Color(0xFF94A3B8) else Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TypePill(mon.primaryType)
                            mon.secondaryType?.let { TypePill(it) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3 Explicit Text Buttons: [Belum] | [Seen] | [Caught]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFF0B0F19))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(7.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TrackerTextSegment(
                                label = "Belum",
                                isSelected = status == TrackerStatus.UNSEEN,
                                activeBg = Color(0xFF334155),
                                activeText = Color.White,
                                onClick = { viewModel.setTrackerStatus(mon.id, TrackerStatus.UNSEEN) }
                            )
                            TrackerTextSegment(
                                label = "Seen",
                                isSelected = status == TrackerStatus.SEEN,
                                activeBg = Color(0xFF0284C7),
                                activeText = Color.White,
                                onClick = { viewModel.setTrackerStatus(mon.id, TrackerStatus.SEEN) }
                            )
                            TrackerTextSegment(
                                label = "Caught",
                                isSelected = status == TrackerStatus.CAUGHT,
                                activeBg = Color(0xFF059669),
                                activeText = Color.White,
                                onClick = { viewModel.setTrackerStatus(mon.id, TrackerStatus.CAUGHT) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerSummaryCard(
    title: String,
    countText: String,
    percentText: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            Text(percentText, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(countText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = Color(0xFF0B0F19)
        )
    }
}

@Composable
private fun RowScope.TrackerTextSegment(
    label: String,
    isSelected: Boolean,
    activeBg: Color,
    activeText: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isSelected) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) activeText else Color(0xFF64748B),
            fontSize = 9.5.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
// SUB-TAB 4: TOP 50 FREE RANKER (WITH REAL POKÉMON NAMES)
// ---------------------------------------------------------------------------
@Composable
fun JohtoModeBFreeRankerView(
    viewModel: PokemonViewModel,
    isShiny: Boolean
) {
    val top50Ids by viewModel.freeRankerTop50.collectAsState()
    val pokeMap = viewModel.pokeMap

    val top50Pokemon = remember(top50Ids, pokeMap) {
        top50Ids.mapNotNull { pokeMap[it] }
    }

    val avgBst = remember(top50Pokemon) {
        if (top50Pokemon.isNotEmpty()) top50Pokemon.map { it.bst }.average().toInt() else 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Johto Top 50 Power Ranker", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Curated ranking of official Johto HGSS Pokémon", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }

                Box(
                    modifier = Modifier
                        .background(NeonAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonAmber, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Avg BST: $avgBst", color = NeonAmber, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(top50Pokemon, key = { _, mon -> mon.id }) { index, mon ->
                val rank = index + 1
                val (rankBadgeBg, rankBadgeText, rankBorder) = when (rank) {
                    1 -> Triple(Color(0x33F59E0B), Color(0xFFF59E0B), Color(0xFFF59E0B))
                    2 -> Triple(Color(0x33E2E8F0), Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    3 -> Triple(Color(0x33CD7F32), Color(0xFFCD7F32), Color(0xFFCD7F32))
                    in 4..10 -> Triple(Color(0x3338BDF8), Color(0xFF38BDF8), Color(0xFF38BDF8))
                    else -> Triple(Color(0xFF0B0F19), Color(0xFF94A3B8), DarkCardBorder)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, rankBorder, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(rankBadgeBg)
                                .border(1.dp, rankBadgeText, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$rank",
                                color = rankBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        AsyncImage(
                            model = mon.spriteUrl(isShiny),
                            contentDescription = mon.name,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { viewModel.openDetailModal(mon) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(mon.name, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TypePill(mon.primaryType)
                                mon.secondaryType?.let { TypePill(it) }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("BST ${mon.bst}", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        IconButton(
                            onClick = { if (index > 0) viewModel.reorderFreeRanker(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = if (index > 0) Color.White else Color(0xFF334155))
                        }

                        IconButton(
                            onClick = { if (index < top50Pokemon.size - 1) viewModel.reorderFreeRanker(index, index + 1) },
                            enabled = index < top50Pokemon.size - 1,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = if (index < top50Pokemon.size - 1) Color.White else Color(0xFF334155))
                        }
                    }
                }
            }
        }
    }
}
