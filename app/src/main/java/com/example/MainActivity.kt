package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PokemonViewModel
import com.example.ui.components.ExportImportDialog
import com.example.ui.components.FlutterSourceDialog
import com.example.ui.components.PokemonDetailModal
import com.example.ui.components.RecentRatingsModal
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class MainAppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    POKEDEX_1025("Pokédex", Icons.Default.CatchingPokemon),
    PARTY_BUILDER("Party Lab", Icons.Default.Groups),
    TYPE_MENTOR("Matchup", Icons.Default.Analytics),
    TRAINER_PROFILE("Trainer & Cup", Icons.Default.WorkspacePremium)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PokemonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: PokemonViewModel) {
    var selectedTab by remember { mutableStateOf(MainAppTab.POKEDEX_1025) }

    val isModern by viewModel.isModern.collectAsState()
    val isShiny by viewModel.isShinyGlobal.collectAsState()
    val detailPokemon by viewModel.detailPokemon.collectAsState()
    val showRecentModal by viewModel.showRecentModal.collectAsState()
    val showExportImportModal by viewModel.showExportImportModal.collectAsState()
    val showFlutterSourceModal by viewModel.showFlutterSourceModal.collectAsState()
    val isPlayingCry by viewModel.isPlayingCry.collectAsState()
    val ratings by viewModel.ratings.collectAsState()
    val partySlots by viewModel.partySlots.collectAsState()
    val recentRatings by viewModel.recentRatings.collectAsState()
    val recentTrackerActions by viewModel.recentTrackerActions.collectAsState()
    val pokedexPageMode by viewModel.pokedexPageMode.collectAsState()
    val liveDetailsMap by viewModel.liveDetailsMap.collectAsState()
    val isLiveSyncing by viewModel.isLiveSyncing.collectAsState()
    val preferredShinyIds by viewModel.preferredShinyIds.collectAsState()

    BackHandler(
        enabled = detailPokemon != null || showRecentModal || showExportImportModal || showFlutterSourceModal || selectedTab != MainAppTab.POKEDEX_1025
    ) {
        when {
            detailPokemon != null -> viewModel.closeDetailModal()
            showRecentModal -> viewModel.openRecentModal(false)
            showExportImportModal -> viewModel.openExportImportModal(false)
            showFlutterSourceModal -> viewModel.openFlutterSourceModal(false)
            selectedTab != MainAppTab.POKEDEX_1025 -> selectedTab = MainAppTab.POKEDEX_1025
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkScaffoldBg),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurfaceHeader,
                tonalElevation = 0.dp,
                modifier = Modifier.border(width = 1.dp, color = SubtleWhiteBorder)
            ) {
                MainAppTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonBlue,
                            selectedTextColor = NeonBlue,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainAppTab.POKEDEX_1025 -> Tab1NationalPokedexScreen(viewModel)
                MainAppTab.PARTY_BUILDER -> Tab3PartyBuilderScreen(viewModel)
                MainAppTab.TYPE_MENTOR -> Tab4PokeTypeMentorScreen(viewModel)
                MainAppTab.TRAINER_PROFILE -> Tab5TrainerProfileScreen(viewModel)
            }
        }
    }

    if (detailPokemon != null) {
        val mon = detailPokemon!!
        val rating = ratings[mon.id]
        val isPartyMember = partySlots.any { it.pokemonId == mon.id }
        val familyMembers = remember(mon.familyId) {
            viewModel.allPokemonList.filter { it.familyId == mon.familyId }
        }

        LaunchedEffect(mon.id) {
            viewModel.fetchPokemonLiveDetails(mon.id)
        }

        PokemonDetailModal(
            pokemon = mon,
            rating = rating,
            familyMembers = familyMembers,
            isModern = isModern,
            isShiny = isShiny,
            isPlayingCry = isPlayingCry,
            liveDetail = liveDetailsMap[mon.id],
            isLiveSyncing = isLiveSyncing,
            isPreferredShiny = preferredShinyIds.contains(mon.id),
            onTogglePreferredShiny = { isPref -> viewModel.togglePreferredShiny(mon.id, isPref) },
            isPartyMember = isPartyMember,
            onTogglePartyMember = { viewModel.togglePartyMembership(mon.id) },
            onPlayCry = { viewModel.playAudioCry(mon.id) },
            onRefreshLiveApi = { viewModel.fetchPokemonLiveDetails(mon.id, true) },
            onSelectFamilyStage = { viewModel.openDetailModal(it) },
            onSetTier = { viewModel.setCustomTier(mon.id, it) },
            onClose = { viewModel.closeDetailModal() }
        )
    }

    if (showRecentModal) {
        RecentRatingsModal(
            recentActions = recentRatings,
            recentTrackerActions = recentTrackerActions,
            currentMode = pokedexPageMode,
            onUndoAction = { viewModel.undoLastRating(it) },
            onUndoTrackerAction = { viewModel.undoTrackerAction(it) },
            onClose = { viewModel.openRecentModal(false) }
        )
    }

    if (showExportImportModal) {
        ExportImportDialog(
            jsonContent = viewModel.exportJsonString(),
            markdownContent = viewModel.exportMarkdownSummary(),
            showdownContent = viewModel.exportShowdown(),
            onImportJson = { viewModel.importJsonString(it) },
            onClose = { viewModel.openExportImportModal(false) }
        )
    }

    if (showFlutterSourceModal) {
        FlutterSourceDialog(
            onClose = { viewModel.openFlutterSourceModal(false) }
        )
    }
}

@Composable
fun GeometricSparkleVector(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val path = Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx, cy, w, cy)
            quadraticTo(cx, cy, cx, h)
            quadraticTo(cx, cy, 0f, cy)
            quadraticTo(cx, cy, cx, 0f)
            close()
        }
        drawPath(path = path, color = tint)
        drawPath(path = path, color = tint.copy(alpha = 0.4f), style = Stroke(width = 1.5f))
    }
}
