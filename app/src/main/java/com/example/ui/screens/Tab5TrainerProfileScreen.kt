package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.*
import com.example.ui.PokemonViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sin

enum class TournamentBracketFilter(val label: String) {
    ALL_LIKED("All Liked"),
    ICON_GOLD("Icon Gold"),
    DESIGN_BLUE("Design Blue"),
    PARTY_RED("Party Red"),
    HGSS_ONLY("HGSS Gen 1-4"),
    XY_KALOS("XY Kalos"),
    PLA_HISUI("PLA Hisui")
}

enum class TrainerAvatarPreset(
    val label: String,
    val capColor: Color,
    val visorColor: Color,
    val jacketColor: Color,
    val hairColor: Color
) {
    ETHAN_GOLD("Ethan (HGSS)", Color(0xFFEAB308), Color(0xFF1E293B), Color(0xFFEF4444), Color(0xFF1E293B)),
    LYRA_SOUL("Lyra (HGSS)", Color(0xFFFFFFFF), Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF78350F)),
    RED_CHAMP("Red (Kanto)", Color(0xFFDC2626), Color(0xFFFFFFFF), Color(0xFFDC2626), Color(0xFF1E293B)),
    LANCE_CAPE("Lance (Master)", Color(0xFF991B1B), Color(0xFFF59E0B), Color(0xFF1E1B4B), Color(0xFFB91C1C))
}

data class FairMatchup(
    val contender1: Pokemon,
    val contender2: Pokemon,
    val fairScore: Int,
    var winner: Pokemon? = null
)

private data class ConfettiParticle(
    val xFraction: Float,
    val speedMultiplier: Float,
    val phaseOffset: Float,
    val widthDp: Float,
    val heightDp: Float,
    val color: Color
)

@Composable
fun Tab5TrainerProfileScreen(
    viewModel: PokemonViewModel,
    modifier: Modifier = Modifier
) {
    val ratings by viewModel.ratings.collectAsState()
    val partySlots by viewModel.partySlots.collectAsState()
    val isShinyGlobal by viewModel.isShinyGlobal.collectAsState()
    val ultimateFavorite by viewModel.ultimateFavorite.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pokeMap = viewModel.pokeMap

    // Hall of Fame Studio State
    var trainerName by remember { mutableStateOf("Mahdi") }
    var trainerId by remember { mutableStateOf("04933") }
    var playTime by remember { mutableStateOf("50:56") }
    var trainerAvatar by remember { mutableStateOf(TrainerAvatarPreset.ETHAN_GOLD) }
    var hofShiny by remember { mutableStateOf(isShinyGlobal) }
    var isRenderingPng by remember { mutableStateOf(false) }
    var pickingHofSlotIndex by remember { mutableStateOf<Int?>(null) }

    // Default 6 Hall of Fame Pokémon (synced from Party Lab if non-empty, or iconic HGSS Champion team)
    var hofTeamIds by remember {
        val partyIds = partySlots.mapNotNull { it.pokemonId }
        val initial = if (partyIds.isNotEmpty()) {
            (partyIds + listOf(157, 181, 130, 196, 248, 249)).distinct().take(6)
        } else {
            listOf(157, 181, 130, 169, 248, 249) // Typhlosion, Ampharos, Gyarados, Crobat, Tyranitar, Lugia
        }
        mutableStateOf(initial)
    }

    val hofPokemonList = remember(hofTeamIds, pokeMap) {
        hofTeamIds.mapNotNull { pokeMap[it] }
    }

    // Staggered celebration flash trigger
    var celebrationTrigger by remember { mutableStateOf(0) }
    val entranceProgress = remember { Animatable(1f) }
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(celebrationTrigger) {
        if (celebrationTrigger > 0) {
            entranceProgress.snapTo(0f)
            entranceProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(0f, animationSpec = tween(650, easing = LinearOutSlowInEasing))
        }
    }

    // Favorite Liked Pokémon
    val likedPokemon = remember(ratings, pokeMap) {
        ratings.filter { it.value.status == RatingStatus.LIKE }
            .mapNotNull { pokeMap[it.key] }
    }

    val totalLiked = likedPokemon.size
    val totalEvaluated = ratings.values.count { it.status != RatingStatus.UNRATED }
    val avgFavoriteBst = remember(likedPokemon) {
        if (likedPokemon.isNotEmpty()) likedPokemon.map { it.bst }.average().toInt() else 0
    }
    val topElementalAffinity = remember(likedPokemon) {
        if (likedPokemon.isNotEmpty()) {
            likedPokemon.flatMap { listOfNotNull(it.primaryType, it.secondaryType) }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key?.displayName ?: "Normal"
        } else "None"
    }

    val typeDistribution = remember(likedPokemon) {
        val totalTypes = likedPokemon.flatMap { listOfNotNull(it.primaryType, it.secondaryType) }
        totalTypes.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(6)
    }

    // Tournament State
    var bracketFilter by remember { mutableStateOf(TournamentBracketFilter.ALL_LIKED) }
    var bracketSize by remember { mutableStateOf(8) }
    var tournamentRound by remember { mutableStateOf(1) }
    var currentMatches by remember { mutableStateOf<List<FairMatchup>>(emptyList()) }
    var tournamentChampion by remember { mutableStateOf<Pokemon?>(null) }

    fun calculateFairMatchScore(mon1: Pokemon, mon2: Pokemon): Int {
        var score = 0
        val tierDiff = abs(mon1.defaultTier.ordinal - mon2.defaultTier.ordinal)
        score += when (tierDiff) {
            0 -> 35
            1 -> 25
            2 -> 15
            else -> 5
        }
        val bstDiff = abs(mon1.bst - mon2.bst).coerceAtMost(250)
        score += (30 * (1.0f - bstDiff / 250.0f)).toInt()
        val types1 = listOfNotNull(mon1.primaryType, mon1.secondaryType)
        val types2 = listOfNotNull(mon2.primaryType, mon2.secondaryType)
        if (types1.any { types2.contains(it) }) score += 15
        if (mon1.isLegendary == mon2.isLegendary) score += 12
        if (mon1.isSingular == mon2.isSingular) score += 8
        return score.coerceIn(20, 100)
    }

    fun startTournamentWithFilter() {
        var pool = when (bracketFilter) {
            TournamentBracketFilter.ALL_LIKED -> likedPokemon
            TournamentBracketFilter.ICON_GOLD -> ratings.filter { it.value.tags.contains(RatingTag.ICON) }.mapNotNull { pokeMap[it.key] }
            TournamentBracketFilter.DESIGN_BLUE -> ratings.filter { it.value.tags.contains(RatingTag.DESIGN) }.mapNotNull { pokeMap[it.key] }
            TournamentBracketFilter.PARTY_RED -> ratings.filter { it.value.tags.contains(RatingTag.PARTY) }.mapNotNull { pokeMap[it.key] }
            TournamentBracketFilter.HGSS_ONLY -> viewModel.allPokemonList.filter { it.generation <= 4 }
            TournamentBracketFilter.XY_KALOS -> viewModel.allPokemonList.filter { it.kalosDexNo != null }
            TournamentBracketFilter.PLA_HISUI -> viewModel.allPokemonList.filter { it.hisuiDexNo != null }
        }
        if (pool.size < bracketSize) {
            pool = viewModel.allPokemonList
        }
        val selected = pool.shuffled().take(bracketSize).toMutableList()
        val matches = mutableListOf<FairMatchup>()
        for (i in 0 until selected.size step 2) {
            if (i + 1 < selected.size) {
                val m1 = selected[i]
                val m2 = selected[i + 1]
                matches.add(FairMatchup(m1, m2, calculateFairMatchScore(m1, m2)))
            }
        }
        currentMatches = matches
        tournamentRound = 1
        tournamentChampion = null
    }

    LaunchedEffect(bracketFilter, bracketSize) {
        startTournamentWithFilter()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkScaffoldBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ===================================================================
        // 1. LEAGUE CHAMPION HALL OF FAME STUDIO (HGSS VICTORY GENERATOR)
        // ===================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, NeonAmber.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HGSS HALL OF FAME VICTORY STUDIO",
                            color = NeonAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "League Champion Celebration & PNG Generator",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Replay Celebration Flash
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, NeonBlue, RoundedCornerShape(6.dp))
                                .clickable { celebrationTrigger++ }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text("Replay Entrance", color = NeonBlue, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Normal / Shiny Sprite Toggle for Hall of Fame
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (hofShiny) NeonAmber.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                .border(1.dp, if (hofShiny) NeonAmber else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { hofShiny = !hofShiny }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (hofShiny) "SHINY: ON" else "SHINY: OFF",
                                color = if (hofShiny) NeonAmber else Color(0xFF94A3B8),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // LIVE 4:3 HGSS HALL OF FAME ANIMATED STAGE
                HgssHallOfFameAnimatedCanvas(
                    trainerName = trainerName.ifBlank { "Mahdi" },
                    trainerId = trainerId.ifBlank { "04933" },
                    playTime = playTime.ifBlank { "50:56" },
                    avatarPreset = trainerAvatar,
                    pokemonTeam = hofPokemonList,
                    isShiny = hofShiny,
                    entranceProgress = entranceProgress.value,
                    flashAlpha = flashAlpha.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Inputs: Trainer Name, ID No., Play Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = trainerName,
                        onValueChange = { trainerName = it.take(14) },
                        label = { Text("Trainer", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = trainerId,
                        onValueChange = { trainerId = it.take(8) },
                        label = { Text("ID No.", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = playTime,
                        onValueChange = { playTime = it.take(8) },
                        label = { Text("Time", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Trainer Avatar Style Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Avatar:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TrainerAvatarPreset.entries.forEach { preset ->
                        val isSel = trainerAvatar == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) NeonAmber.copy(alpha = 0.2f) else Color(0xFF0B0F19))
                                .border(1.dp, if (isSel) NeonAmber else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { trainerAvatar = preset }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.label,
                                color = if (isSel) NeonAmber else Color(0xFFCBD5E1),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6 Hall of Fame Team Slots + "Sync from Party Lab" Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHAMPION ROSTER (TAP SLOT TO CHANGE POKÉMON):",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                val partyIds = partySlots.mapNotNull { it.pokemonId }
                                if (partyIds.isNotEmpty()) {
                                    hofTeamIds = (partyIds + hofTeamIds).distinct().take(6)
                                    celebrationTrigger++
                                    Toast.makeText(context, "Synced active roster from Party Lab", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Party Lab is empty — add Pokémon in Party Lab first", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("Use Party Lab Team", color = NeonBlue, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (idx in 0 until 6) {
                        val mon = hofPokemonList.getOrNull(idx)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0B0F19))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable { pickingHofSlotIndex = idx }
                                .padding(vertical = 5.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (mon != null) {
                                AsyncImage(
                                    model = mon.spriteUrl(hofShiny),
                                    contentDescription = mon.name,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = mon.name,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                    Text("+", color = Color(0xFF64748B), fontSize = 14.sp)
                                }
                                Text("Slot ${idx + 1}", color = Color(0xFF64748B), fontSize = 8.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Render & Download Victory Image (PNG) Button
                Button(
                    onClick = {
                        if (!isRenderingPng) {
                            isRenderingPng = true
                            scope.launch {
                                val savedPath = renderAndSaveHallOfFamePng(
                                    context = context,
                                    trainerName = trainerName.ifBlank { "Mahdi" },
                                    trainerId = trainerId.ifBlank { "04933" },
                                    playTime = playTime.ifBlank { "50:56" },
                                    avatarPreset = trainerAvatar,
                                    pokemonTeam = hofPokemonList,
                                    isShiny = hofShiny
                                )
                                isRenderingPng = false
                                if (savedPath != null) {
                                    Toast.makeText(
                                        context,
                                        "Victory PNG saved: $savedPath",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to render Victory PNG",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonAmber,
                        contentColor = Color(0xFF0B0F19)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_render_hof_png")
                ) {
                    if (isRenderingPng) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF0B0F19),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rendering High-Res Victory PNG...", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Render & Download Victory Image (PNG)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // ===================================================================
        // 2. TRAINER PASSPORT & PROFILE SUMMARY
        // ===================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TRAINER PASSPORT & ANALYTICS", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(trainerName.ifBlank { "Mahdi" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("ID: ${trainerId.ifBlank { "04933" }}  •  Johto & National Dex Tactician", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val badgeMon = ultimateFavorite ?: tournamentChampion ?: likedPokemon.firstOrNull()
                    if (badgeMon != null) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B0F19))
                                .border(1.5.dp, NeonAmber, CircleShape)
                                .clickable { viewModel.playAudioCry(badgeMon.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = badgeMon.spriteUrl(isShinyGlobal),
                                contentDescription = badgeMon.name,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrainerStatItem("Total Liked", "$totalLiked", NeonAmber)
                    TrainerStatItem("Top Affinity", topElementalAffinity, NeonBlue)
                    TrainerStatItem("Avg BST", "$avgFavoriteBst", NeonEmerald)
                    TrainerStatItem("Evaluated", "$totalEvaluated", Color(0xFFA855F7))
                }

                if (typeDistribution.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ELEMENTAL AFFINITY DISTRIBUTION", color = Color(0xFF94A3B8), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    val maxCount = typeDistribution.first().second.coerceAtLeast(1)
                    typeDistribution.forEach { (type, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.displayName, color = Color(0xFFCBD5E1), fontSize = 10.sp, modifier = Modifier.width(65.dp))
                            LinearProgressIndicator(
                                progress = { count.toFloat() / maxCount },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = type.color,
                                trackColor = Color(0xFF0B0F19)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ===================================================================
        // 3. KNOCKOUT TOURNAMENT ARENA (FAIR MATCHMAKING, NO EMOJIS)
        // ===================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("KNOCKOUT TOURNAMENT CUP", color = NeonBlue, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = if (tournamentChampion != null) "Champion Crowned: ${tournamentChampion!!.name}" else "Round $tournamentRound • Select Winner to Advance",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = { startTournamentWithFilter() },
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF0B0F19), CircleShape)
                            .border(1.dp, DarkCardBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bracket Size:", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(4, 8, 16, 32).forEach { s ->
                            val isSel = bracketSize == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) NeonBlue else Color(0xFF0B0F19))
                                    .border(1.dp, if (isSel) NeonBlue else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { bracketSize = s }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("$s", color = if (isSel) Color(0xFF0B0F19) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TournamentBracketFilter.entries.forEach { f ->
                        val isSel = bracketFilter == f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) NeonAmber else Color(0xFF0B0F19))
                                .border(1.dp, if (isSel) NeonAmber else DarkCardBorder, RoundedCornerShape(6.dp))
                                .clickable { bracketFilter = f }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(f.label, color = if (isSel) Color(0xFF0B0F19) else Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tournamentChampion != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B0F19), RoundedCornerShape(12.dp))
                            .border(1.5.dp, TagGoldBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOURNAMENT CHAMPION", color = TagGoldBorder, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = tournamentChampion!!.officialArtworkUrl(),
                            contentDescription = tournamentChampion!!.name,
                            modifier = Modifier.size(84.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tournamentChampion!!.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text("BST ${tournamentChampion!!.bst} • Assigned as #1 Ultimate Favorite", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { startTournamentWithFilter() },
                            colors = ButtonDefaults.buttonColors(containerColor = TagGoldBorder, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Start New Bracket", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currentMatches.forEachIndexed { matchIdx, match ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0B0F19))
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Match #${matchIdx + 1}", color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Parity Score: ${match.fairScore}/100",
                                        color = if (match.fairScore >= 75) NeonEmerald else NeonAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ContenderButton(
                                        mon = match.contender1,
                                        isWinner = match.winner == match.contender1,
                                        isShiny = isShinyGlobal,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val updated = currentMatches.toMutableList()
                                        updated[matchIdx] = match.copy(winner = match.contender1)
                                        currentMatches = updated
                                        checkFairRound(updated, tournamentRound) { nextMatches, nextRound, champ ->
                                            currentMatches = nextMatches
                                            tournamentRound = nextRound
                                            if (champ != null) {
                                                tournamentChampion = champ
                                                viewModel.setUltimateFavorite(champ)
                                            }
                                        }
                                    }

                                    Text("VS", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp))

                                    ContenderButton(
                                        mon = match.contender2,
                                        isWinner = match.winner == match.contender2,
                                        isShiny = isShinyGlobal,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val updated = currentMatches.toMutableList()
                                        updated[matchIdx] = match.copy(winner = match.contender2)
                                        currentMatches = updated
                                        checkFairRound(updated, tournamentRound) { nextMatches, nextRound, champ ->
                                            currentMatches = nextMatches
                                            tournamentRound = nextRound
                                            if (champ != null) {
                                                tournamentChampion = champ
                                                viewModel.setUltimateFavorite(champ)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===================================================================
        // 4. DATA BACKUP & ARCHITECTURE SOURCE TOOLS (MOVED FROM TOP BAR)
        // ===================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DATA STORAGE, BACKUP & MULTIPLATFORM SOURCE",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.openExportImportModal(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = NeonEmerald),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_export_import")
                    ) {
                        Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup / Restore JSON", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.openFlutterSourceModal(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = NeonBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_flutter_source")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Architecture Source", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Hall of Fame Slot Pokémon Picker Modal
    pickingHofSlotIndex?.let { slotIdx ->
        var searchQ by remember { mutableStateOf("") }
        val filteredMons = remember(searchQ, viewModel.allPokemonList) {
            if (searchQ.isBlank()) viewModel.allPokemonList
            else {
                val q = searchQ.trim().lowercase()
                viewModel.allPokemonList.filter { it.name.lowercase().contains(q) || it.id.toString() == q }
            }
        }

        Dialog(onDismissRequest = { pickingHofSlotIndex = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonAmber, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Hall of Fame Slot #${slotIdx + 1}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = { pickingHofSlotIndex = null }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQ,
                        onValueChange = { searchQ = it },
                        placeholder = { Text("Search Pokémon name or #...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAmber,
                            unfocusedBorderColor = DarkCardBorder
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredMons, key = { it.id }) { mon ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceCard)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val updated = hofTeamIds.toMutableList()
                                        while (updated.size < 6) updated.add(157)
                                        updated[slotIdx] = mon.id
                                        hofTeamIds = updated
                                        pickingHofSlotIndex = null
                                        celebrationTrigger++
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = mon.spriteUrl(hofShiny), contentDescription = null, modifier = Modifier.size(34.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${mon.name} (${mon.formattedShortId()})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("BST ${mon.bst}", color = NeonAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===========================================================================
// HGSS HALL OF FAME 4:3 ANIMATED STAGE (SPOTLIGHTS, CONFETTI, V-FORMATION)
// ===========================================================================
@Composable
fun HgssHallOfFameAnimatedCanvas(
    trainerName: String,
    trainerId: String,
    playTime: String,
    avatarPreset: TrainerAvatarPreset,
    pokemonTeam: List<Pokemon>,
    isShiny: Boolean,
    entranceProgress: Float,
    flashAlpha: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hof_anim")

    val spotlightPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spotlights"
    )

    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti"
    )

    val confettiParticles = remember {
        val palette = listOf(
            Color(0xFFEF4444), // Red
            Color(0xFFFACC15), // Yellow
            Color(0xFF22C55E), // Green
            Color(0xFF38BDF8), // Cyan
            Color(0xFFE879F9), // Magenta
            Color(0xFFFFFFFF)  // White
        )
        List(38) { idx ->
            ConfettiParticle(
                xFraction = ((idx * 37) % 100) / 100f,
                speedMultiplier = 0.7f + ((idx * 13) % 7) * 0.1f,
                phaseOffset = ((idx * 29) % 100) / 100f,
                widthDp = if (idx % 2 == 0) 6f else 4.5f,
                heightDp = if (idx % 3 == 0) 3.5f else 5f,
                color = palette[idx % palette.size]
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, Color(0xFF475569), RoundedCornerShape(10.dp))
            .testTag("hgss_hof_stage")
    ) {
        val stageW = maxWidth
        val stageH = maxHeight

        // 1. Background Canvas: #0048B8 Royal Blue + Striped Polka Dots + 4 Swinging Spotlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Royal Blue HGSS Background
            drawRect(color = Color(0xFF0048B8))

            // Horizontal Striped White-Yellow Polka-Dot Circles (HGSS Hall of Fame pattern)
            val circlePositions = listOf(
                Triple(w * 0.16f, h * 0.30f, w * 0.11f),
                Triple(w * 0.84f, h * 0.30f, w * 0.11f),
                Triple(w * 0.50f, h * 0.25f, w * 0.14f),
                Triple(w * 0.22f, h * 0.68f, w * 0.12f),
                Triple(w * 0.78f, h * 0.68f, w * 0.12f),
                Triple(w * 0.50f, h * 0.62f, w * 0.16f)
            )
            circlePositions.forEach { (cx, cy, radius) ->
                val stripeCount = 9
                val stripeStep = (radius * 2f) / stripeCount
                for (s in 0 until stripeCount) {
                    if (s % 2 == 0) {
                        val yOff = -radius + s * stripeStep
                        val dy = abs(yOff + stripeStep / 2f)
                        if (dy < radius) {
                            val halfChord = kotlin.math.sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
                            drawRect(
                                color = Color(0x38FFFACD),
                                topLeft = Offset(cx - halfChord, cy + yOff),
                                size = Size(halfChord * 2f, stripeStep * 0.65f)
                            )
                        }
                    }
                }
            }

            // 4 Swinging Translucent Yellow Spotlight Cones from Bottom & Top
            val beamOrigins = listOf(w * 0.15f, w * 0.38f, w * 0.62f, w * 0.85f)
            beamOrigins.forEachIndexed { idx, originX ->
                val swing = sin(spotlightPhase + idx * 1.3f) * (w * 0.16f)
                val topTargetX = (originX + swing).coerceIn(0f, w)
                val beamHalfWidth = w * 0.085f

                val path = Path().apply {
                    moveTo(originX - 8f, h * 0.87f)
                    lineTo(originX + 8f, h * 0.87f)
                    lineTo(topTargetX + beamHalfWidth, h * 0.13f)
                    lineTo(topTargetX - beamHalfWidth, h * 0.13f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0x33FFEB3B)
                )
            }
        }

        // 2. V-Formation Layered Pokémon Sprites + Center HGSS Trainer Pixel Sprite
        // Back Row: Slot #4 (index 4, Top-Left) & Slot #5 (index 5, Top-Right)
        // Mid Row:  Slot #2 (index 2, Mid-Left) & Slot #3 (index 3, Mid-Right)
        // Front Row: Slot #0 (index 0, Front-Left) & Slot #1 (index 1, Front-Right)
        val slotOffsets = listOf(
            // Front-Left (#1) & Front-Right (#2)
            Triple(0.23f, 0.62f, 0.23f),
            Triple(0.77f, 0.62f, 0.23f),
            // Mid-Left (#3) & Mid-Right (#4)
            Triple(0.14f, 0.44f, 0.21f),
            Triple(0.86f, 0.44f, 0.21f),
            // Back-Left (#5) & Back-Right (#6)
            Triple(0.32f, 0.33f, 0.19f),
            Triple(0.68f, 0.33f, 0.19f)
        )

        // Draw Back -> Mid -> Front order so foreground overlaps cleanly
        val drawOrder = listOf(4, 5, 2, 3, 0, 1)
        drawOrder.forEach { slotIdx ->
            val mon = pokemonTeam.getOrNull(slotIdx)
            if (mon != null) {
                val (xFrac, yFrac, sizeFrac) = slotOffsets[slotIdx]
                val spriteSize = stageW * sizeFrac * entranceProgress.coerceIn(0.2f, 1f)
                val xPos = stageW * xFrac - spriteSize / 2f
                val yPos = stageH * yFrac - spriteSize / 2f

                AsyncImage(
                    model = mon.spriteUrl(isShiny),
                    contentDescription = mon.name,
                    modifier = Modifier
                        .offset(x = xPos, y = yPos)
                        .size(spriteSize),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Center Focal Point: HGSS Pixel-Art Trainer Sprite
        val trainerW = stageW * 0.18f
        val trainerH = stageH * 0.30f
        HgssPixelTrainerCanvas(
            preset = avatarPreset,
            modifier = Modifier
                .offset(
                    x = stageW * 0.50f - trainerW / 2f,
                    y = stageH * 0.54f - trainerH / 2f
                )
                .size(width = trainerW, height = trainerH)
        )

        // 3. Foreground Layer: Falling Pixel Confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            confettiParticles.forEach { p ->
                val yNorm = (confettiProgress * p.speedMultiplier + p.phaseOffset) % 1.0f
                val swayX = sin((yNorm * 6.28f) + p.phaseOffset * 10f) * 10f
                val px = (p.xFraction * w + swayX).coerceIn(4f, w - 12f)
                val py = h * 0.13f + yNorm * (h * 0.74f)
                drawRect(
                    color = p.color,
                    topLeft = Offset(px, py),
                    size = Size(p.widthDp.dp.toPx(), p.heightDp.dp.toPx())
                )
            }
        }

        // 4. Top Retro Banner: "League Champion! Congratulations!"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(stageH * 0.135f)
                .background(Color(0xFF282C34))
                .border(1.5.dp, Color(0xFF94A3B8))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "League Champion! Congratulations!",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(2f, 2f),
                        blurRadius = 1f
                    )
                ),
                maxLines = 1
            )
        }

        // 5. Bottom Retro Banner: "Mahdi   ID: 04933   Time: 50:56"
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(stageH * 0.13f)
                .background(Color(0xFF282C34))
                .border(1.5.dp, Color(0xFF94A3B8))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trainerName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 1f))
                )
                Text(
                    text = "ID: $trainerId",
                    color = Color(0xFFFDE047),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 1f))
                )
                Text(
                    text = "Time: $playTime",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 1f))
                )
            }
        }

        // 6. Camera Celebration White Flash Overlay
        if (flashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }
    }
}

@Composable
private fun HgssPixelTrainerCanvas(
    preset: TrainerAvatarPreset,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val u = w / 12f
        val v = h / 18f

        // Shadow oval at feet
        drawOval(
            color = Color(0x55000000),
            topLeft = Offset(u * 2f, v * 15.8f),
            size = Size(u * 8f, v * 1.8f)
        )

        // Boots & Pants
        drawRect(color = Color(0xFF1E293B), topLeft = Offset(u * 3.5f, v * 12.5f), size = Size(u * 2.2f, v * 4f))
        drawRect(color = Color(0xFF1E293B), topLeft = Offset(u * 6.3f, v * 12.5f), size = Size(u * 2.2f, v * 4f))
        drawRect(color = preset.capColor, topLeft = Offset(u * 3.2f, v * 15.5f), size = Size(u * 2.5f, v * 1.3f))
        drawRect(color = preset.capColor, topLeft = Offset(u * 6.3f, v * 15.5f), size = Size(u * 2.5f, v * 1.3f))

        // Jacket / Torso
        drawRect(color = preset.jacketColor, topLeft = Offset(u * 2.5f, v * 7.5f), size = Size(u * 7f, v * 5.2f))
        // Collar / Inner Trim
        drawRect(color = preset.capColor, topLeft = Offset(u * 4.5f, v * 7.5f), size = Size(u * 3f, v * 2.2f))

        // Arms & Hands
        drawRect(color = preset.jacketColor, topLeft = Offset(u * 1.3f, v * 7.8f), size = Size(u * 1.5f, v * 3.5f))
        drawRect(color = preset.jacketColor, topLeft = Offset(u * 9.2f, v * 7.8f), size = Size(u * 1.5f, v * 3.5f))
        drawRect(color = Color(0xFFFDE68A), topLeft = Offset(u * 1.3f, v * 11.0f), size = Size(u * 1.5f, v * 1.3f))
        drawRect(color = Color(0xFFFDE68A), topLeft = Offset(u * 9.2f, v * 11.0f), size = Size(u * 1.5f, v * 1.3f))

        // Pokéball held in right hand
        drawCircle(
            color = Color(0xFFEF4444),
            radius = u * 0.9f,
            center = Offset(u * 10.2f, v * 11.5f)
        )
        drawCircle(
            color = Color.White,
            radius = u * 0.35f,
            center = Offset(u * 10.2f, v * 11.5f)
        )

        // Head & Hair
        drawRect(color = preset.hairColor, topLeft = Offset(u * 2.8f, v * 3.2f), size = Size(u * 6.4f, v * 4.2f))
        drawRect(color = Color(0xFFFDE68A), topLeft = Offset(u * 3.4f, v * 4.0f), size = Size(u * 5.2f, v * 3.4f))

        // Eyes
        drawRect(color = Color(0xFF0F172A), topLeft = Offset(u * 4.2f, v * 5.0f), size = Size(u * 0.9f, v * 1.1f))
        drawRect(color = Color(0xFF0F172A), topLeft = Offset(u * 6.9f, v * 5.0f), size = Size(u * 0.9f, v * 1.1f))

        // Iconic HGSS Cap & Visor
        drawRect(color = preset.capColor, topLeft = Offset(u * 2.6f, v * 1.6f), size = Size(u * 6.8f, v * 2.2f))
        drawRect(color = preset.visorColor, topLeft = Offset(u * 2.2f, v * 3.1f), size = Size(u * 7.6f, v * 0.9f))
    }
}

// ===========================================================================
// BITMAP RENDERER & PNG SAVER FOR HGSS HALL OF FAME VICTORY IMAGE
// ===========================================================================
private suspend fun renderAndSaveHallOfFamePng(
    context: Context,
    trainerName: String,
    trainerId: String,
    playTime: String,
    avatarPreset: TrainerAvatarPreset,
    pokemonTeam: List<Pokemon>,
    isShiny: Boolean
): String? = withContext(Dispatchers.IO) {
    try {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Royal Blue Background (#0048B8)
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.rgb(0, 72, 184)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 2. Striped Polka-Dot Circles
        paint.color = AndroidColor.argb(58, 255, 250, 205)
        val circles = listOf(
            Triple(width * 0.16f, height * 0.30f, width * 0.11f),
            Triple(width * 0.84f, height * 0.30f, width * 0.11f),
            Triple(width * 0.50f, height * 0.25f, width * 0.14f),
            Triple(width * 0.22f, height * 0.68f, width * 0.12f),
            Triple(width * 0.78f, height * 0.68f, width * 0.12f),
            Triple(width * 0.50f, height * 0.62f, width * 0.16f)
        )
        circles.forEach { (cx, cy, radius) ->
            val stripes = 9
            val step = (radius * 2f) / stripes
            for (s in 0 until stripes) {
                if (s % 2 == 0) {
                    val yOff = -radius + s * step
                    val dy = abs(yOff + step / 2f)
                    if (dy < radius) {
                        val halfChord = kotlin.math.sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
                        canvas.drawRect(cx - halfChord, cy + yOff, cx + halfChord, cy + yOff + step * 0.65f, paint)
                    }
                }
            }
        }

        // 3. 4 Spotlight Cones
        paint.color = AndroidColor.argb(55, 255, 235, 59)
        val origins = listOf(width * 0.15f, width * 0.38f, width * 0.62f, width * 0.85f)
        origins.forEachIndexed { idx, ox ->
            val topX = ox + (if (idx % 2 == 0) 45f else -45f)
            val path = AndroidPath().apply {
                moveTo(ox - 12f, height * 0.87f)
                lineTo(ox + 12f, height * 0.87f)
                lineTo(topX + 68f, height * 0.13f)
                lineTo(topX - 68f, height * 0.13f)
                close()
            }
            canvas.drawPath(path, paint)
        }

        // 4. Load & Draw 6 Pokémon Sprites in V-Formation
        val imageLoader = ImageLoader(context)
        val slotCoords = listOf(
            Triple(0.23f, 0.62f, 185),
            Triple(0.77f, 0.62f, 185),
            Triple(0.14f, 0.44f, 168),
            Triple(0.86f, 0.44f, 168),
            Triple(0.32f, 0.33f, 152),
            Triple(0.68f, 0.33f, 152)
        )
        val drawOrder = listOf(4, 5, 2, 3, 0, 1)
        for (slotIdx in drawOrder) {
            val mon = pokemonTeam.getOrNull(slotIdx) ?: continue
            val (xf, yf, sz) = slotCoords[slotIdx]
            val cx = (width * xf).toInt()
            val cy = (height * yf).toInt()
            val half = sz / 2

            val req = ImageRequest.Builder(context)
                .data(mon.spriteUrl(isShiny))
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(req)
            val spriteBmp = (result as? SuccessResult)?.drawable?.let { drawable ->
                (drawable as? BitmapDrawable)?.bitmap
            }
            if (spriteBmp != null) {
                val dst = Rect(cx - half, cy - half, cx + half, cy + half)
                canvas.drawBitmap(spriteBmp, null, dst, null)
            } else {
                // Fallback emblem circle with Pokémon name if offline
                paint.color = AndroidColor.argb(180, 17, 24, 39)
                canvas.drawCircle(cx.toFloat(), cy.toFloat(), half * 0.7f, paint)
                paint.color = AndroidColor.WHITE
                paint.textSize = 18f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(mon.name, cx.toFloat(), cy.toFloat(), paint)
            }
        }

        // 5. Draw Center Trainer Sprite on Bitmap
        val tx = width * 0.42f
        val ty = height * 0.40f
        val tw = width * 0.16f
        val th = height * 0.28f
        val u = tw / 12f
        val v = th / 18f
        paint.color = AndroidColor.rgb(30, 41, 59)
        canvas.drawRect(tx + u * 3.5f, ty + v * 12.5f, tx + u * 5.7f, ty + v * 16.5f, paint)
        canvas.drawRect(tx + u * 6.3f, ty + v * 12.5f, tx + u * 8.5f, ty + v * 16.5f, paint)
        paint.color = AndroidColor.rgb(239, 68, 68)
        canvas.drawRect(tx + u * 2.5f, ty + v * 7.5f, tx + u * 9.5f, ty + v * 12.7f, paint)
        paint.color = AndroidColor.rgb(253, 230, 138)
        canvas.drawRect(tx + u * 3.4f, ty + v * 4.0f, tx + u * 8.6f, ty + v * 7.4f, paint)
        paint.color = AndroidColor.rgb(234, 179, 8)
        canvas.drawRect(tx + u * 2.6f, ty + v * 1.6f, tx + u * 9.4f, ty + v * 3.8f, paint)
        let {
            val unused = avatarPreset
        }

        // 6. Draw Pixel Confetti
        val confColors = intArrayOf(
            AndroidColor.rgb(239, 68, 68),
            AndroidColor.rgb(250, 204, 21),
            AndroidColor.rgb(34, 197, 94),
            AndroidColor.rgb(56, 189, 248),
            AndroidColor.rgb(232, 121, 249),
            AndroidColor.WHITE
        )
        for (i in 0 until 36) {
            paint.color = confColors[i % confColors.size]
            val px = ((i * 53) % 760) + 20f
            val py = 90f + ((i * 41) % 410)
            canvas.drawRect(px, py, px + 10f, py + 6f, paint)
        }

        // 7. Top & Bottom Retro Banners
        paint.color = AndroidColor.rgb(40, 44, 52)
        canvas.drawRect(0f, 0f, width.toFloat(), 76f, paint)
        canvas.drawRect(0f, height - 76f, width.toFloat(), height.toFloat(), paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = AndroidColor.rgb(148, 163, 184)
        canvas.drawRect(2f, 2f, width - 2f, 74f, paint)
        canvas.drawRect(2f, height - 74f, width - 2f, height - 2f, paint)

        // Banner Text
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 26f
        paint.textAlign = Paint.Align.CENTER

        // Drop shadow + Top Text
        paint.color = AndroidColor.BLACK
        canvas.drawText("League Champion! Congratulations!", width / 2f + 2f, 48f + 2f, paint)
        paint.color = AndroidColor.WHITE
        canvas.drawText("League Champion! Congratulations!", width / 2f, 48f, paint)

        // Bottom Text
        val footerText = "$trainerName     ID: $trainerId     Time: $playTime"
        paint.textSize = 24f
        paint.color = AndroidColor.BLACK
        canvas.drawText(footerText, width / 2f + 2f, height - 28f + 2f, paint)
        paint.color = AndroidColor.rgb(253, 224, 71)
        canvas.drawText(footerText, width / 2f, height - 28f, paint)

        // Save PNG to MediaStore / Pictures
        val fileName = "PokeMaster_HallOfFame_${trainerName.replace(" ", "_")}_${System.currentTimeMillis() / 1000}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PokeMasterHub")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return@withContext "Pictures/PokeMasterHub/$fileName"
            }
        }

        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val outFile = File(fallbackDir, fileName)
        FileOutputStream(outFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return@withContext outFile.name
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun checkFairRound(
    matches: List<FairMatchup>,
    currentRound: Int,
    onNext: (List<FairMatchup>, Int, Pokemon?) -> Unit
) {
    if (matches.all { it.winner != null }) {
        val winners = matches.map { it.winner!! }
        if (winners.size == 1) {
            onNext(emptyList(), currentRound + 1, winners.first())
        } else {
            val nextMatches = mutableListOf<FairMatchup>()
            for (i in winners.indices step 2) {
                if (i + 1 < winners.size) {
                    val m1 = winners[i]
                    val m2 = winners[i + 1]
                    nextMatches.add(FairMatchup(m1, m2, 85))
                }
            }
            onNext(nextMatches, currentRound + 1, null)
        }
    }
}

@Composable
fun ContenderButton(
    mon: Pokemon,
    isWinner: Boolean,
    isShiny: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isWinner) Color(0xFF064E3B) else DarkSurfaceCard)
            .border(1.dp, if (isWinner) Color(0xFF10B981) else DarkCardBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = mon.spriteUrl(isShiny), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = mon.name,
                    color = if (isWinner) Color(0xFF34D399) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("BST ${mon.bst}", color = Color(0xFF94A3B8), fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun TrainerStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
