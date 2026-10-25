package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ai.TacticalCoachAI
import com.example.data.model.*
import com.example.ui.PokemonViewModel
import com.example.ui.components.TypePill
import com.example.ui.theme.*

@Composable
fun Tab3PartyBuilderScreen(
    viewModel: PokemonViewModel,
    modifier: Modifier = Modifier
) {
    val partySlots by viewModel.partySlots.collectAsState()
    val isModern by viewModel.isModern.collectAsState()
    val aiChatMessages by viewModel.aiChatMessages.collectAsState()

    var showAiChatModal by remember { mutableStateOf(false) }
    var pokemonPickerSlotIndex by remember { mutableStateOf<Int?>(null) }
    // Pair(slot, moveIndex: 1..4 for Moves, 5 for Secret Type)
    var typePickerTarget by remember { mutableStateOf<Pair<PartySlot, Int>?>(null) }

    // Collapsible / Compact 2x3 Party Grid toggle
    var isCompactPartyGrid by remember { mutableStateOf(false) }
    var expandedSlotInCompactMode by remember { mutableStateOf<Int?>(0) }

    // Interactive drill-down inspection states for Panel A and Panel B
    var selectedAuditTypeDefense by remember { mutableStateOf<PokemonType?>(null) }
    var selectedAuditTypeOffense by remember { mutableStateOf<PokemonType?>(null) }

    val pokeMap = viewModel.pokeMap
    val activeTypes = remember(isModern) { PokemonType.activeTypes(isModern) }

    val activeSlotsWithMon = remember(partySlots, pokeMap) {
        partySlots.mapNotNull { slot ->
            slot.pokemonId?.let { id -> pokeMap[id]?.let { mon -> Pair(slot, mon) } }
        }
    }
    val activeMembers = remember(activeSlotsWithMon) {
        activeSlotsWithMon.map { it.second }
    }

    // Panel A: Defensive Profile based on Pokémon body types
    val defensiveProfile = remember(partySlots, isModern) {
        viewModel.calculateTeamDefensiveProfile()
    }
    val stackedWeaknesses = remember(defensiveProfile) {
        defensiveProfile.filter { it.totalWeak >= 3 }
    }
    val dangerThreats = remember(defensiveProfile) {
        defensiveProfile.filter { it.totalWeak > 0 }
            .sortedByDescending { it.weak4xCount * 3 + it.weak2xCount * 2 - (if (it.resistOrImmuneCount > 0) 1 else 0) }
    }
    val shieldTypes = remember(defensiveProfile) {
        defensiveProfile.filter { it.resistOrImmuneCount > 0 }
            .sortedByDescending { it.resistOrImmuneCount }
    }
    val neutralTypes = remember(defensiveProfile) {
        defensiveProfile.filter { it.totalWeak == 0 && it.resistOrImmuneCount == 0 }.map { it.type }
    }

    // Calculate Team Defense Grade
    val defenseGradePair = remember(defensiveProfile, activeMembers.size) {
        if (activeMembers.isEmpty()) {
            Pair("—", Color(0xFF64748B))
        } else {
            val unshieldedThreats = defensiveProfile.count { it.totalWeak > 0 && it.resistOrImmuneCount == 0 }
            val stackedCount = stackedWeaknesses.size
            when {
                stackedCount == 0 && unshieldedThreats <= 2 -> Pair("Grade S", NeonEmerald)
                stackedCount == 0 && unshieldedThreats <= 5 -> Pair("Grade A", Color(0xFF38BDF8))
                stackedCount <= 1 && unshieldedThreats <= 8 -> Pair("Grade B", NeonAmber)
                stackedCount <= 2 -> Pair("Grade C", Color(0xFFF97316))
                else -> Pair("Grade D", Color(0xFFEF4444))
            }
        }
    }

    // Panel B: True Offensive Coverage from 4 Moves + Secret Type
    val coverageBreakdown = remember(partySlots, isModern) {
        viewModel.calculateDetailedOffensiveCoverage()
    }
    val recommendedMovesForBlindSpots = remember(coverageBreakdown.blindSpots, isModern) {
        TacticalCoachAI.recommendMoveTypesForBlindSpots(coverageBreakdown.blindSpots, isModern)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkScaffoldBg)
    ) {
        // Clean Party Lab Header with View Mode Toggle + AI Coach Button
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "6-Slot Tactical Party Lab",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "${activeMembers.size}/6 Active • ${coverageBreakdown.coveredTypes.size}/${activeTypes.size} Types Covered",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Compact 2x3 Grid vs Full Cards Toggle
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCompactPartyGrid) NeonAmber.copy(alpha = 0.2f) else Color(0xFF1E293B))
                            .border(
                                1.dp,
                                if (isCompactPartyGrid) NeonAmber else DarkCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { isCompactPartyGrid = !isCompactPartyGrid }
                            .padding(horizontal = 9.dp)
                            .testTag("toggle_party_compact_grid"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCompactPartyGrid) "2×3 Grid" else "Full Cards",
                            color = if (isCompactPartyGrid) NeonAmber else Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // AI Coach Interactive Chat Button
                    Button(
                        onClick = { showAiChatModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonBlue,
                            contentColor = Color(0xFF0B0F19)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_ai_coach")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Coach",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Scrollable Content: 6 Party Slots + Panel A (Defensive) + Panel B (Offensive)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 6-SLOT PARTY CONFIGURATOR (2x3 Compact Grid OR Full List)
            if (isCompactPartyGrid) {
                item {
                    CompactSixSlotOverviewGrid(
                        partySlots = partySlots,
                        pokeMap = pokeMap,
                        selectedSlotIndex = expandedSlotInCompactMode,
                        onSelectSlot = { idx ->
                            expandedSlotInCompactMode = if (expandedSlotInCompactMode == idx) null else idx
                        },
                        onPickPokemonForEmpty = { idx ->
                            expandedSlotInCompactMode = idx
                            pokemonPickerSlotIndex = idx
                        }
                    )
                }

                expandedSlotInCompactMode?.let { activeIdx ->
                    val slot = partySlots.getOrNull(activeIdx)
                    if (slot != null) {
                        item(key = "compact_expanded_${slot.slotIndex}") {
                            val mon = slot.pokemonId?.let { pokeMap[it] }
                            PartySlotTacticalCard(
                                slot = slot,
                                pokemon = mon,
                                onToggleShiny = { viewModel.togglePartySlotShiny(slot.slotIndex) },
                                onSelectPokemon = { pokemonPickerSlotIndex = slot.slotIndex },
                                onClearSlot = { viewModel.assignPokemonToPartySlot(slot.slotIndex, null) },
                                onPickMoveType = { moveIdx -> typePickerTarget = Pair(slot, moveIdx) },
                                onToggleMoveCategory = { moveIdx ->
                                    val updated = when (moveIdx) {
                                        1 -> slot.copy(move1Category = if (slot.move1Category == "Special") "Physical" else "Special")
                                        2 -> slot.copy(move2Category = if (slot.move2Category == "Special") "Physical" else "Special")
                                        3 -> slot.copy(move3Category = if (slot.move3Category == "Special") "Physical" else "Special")
                                        4 -> slot.copy(move4Category = if (slot.move4Category == "Special") "Physical" else "Special")
                                        else -> slot.copy(secretCategory = if (slot.secretCategory == "Special") "Physical" else "Special")
                                    }
                                    viewModel.updatePartySlot(updated)
                                },
                                onOpenDetail = { if (mon != null) viewModel.openDetailModal(mon) }
                            )
                        }
                    }
                }
            } else {
                items(partySlots, key = { it.slotIndex }) { slot ->
                    val mon = slot.pokemonId?.let { pokeMap[it] }
                    PartySlotTacticalCard(
                        slot = slot,
                        pokemon = mon,
                        onToggleShiny = { viewModel.togglePartySlotShiny(slot.slotIndex) },
                        onSelectPokemon = { pokemonPickerSlotIndex = slot.slotIndex },
                        onClearSlot = { viewModel.assignPokemonToPartySlot(slot.slotIndex, null) },
                        onPickMoveType = { moveIdx -> typePickerTarget = Pair(slot, moveIdx) },
                        onToggleMoveCategory = { moveIdx ->
                            val updated = when (moveIdx) {
                                1 -> slot.copy(move1Category = if (slot.move1Category == "Special") "Physical" else "Special")
                                2 -> slot.copy(move2Category = if (slot.move2Category == "Special") "Physical" else "Special")
                                3 -> slot.copy(move3Category = if (slot.move3Category == "Special") "Physical" else "Special")
                                4 -> slot.copy(move4Category = if (slot.move4Category == "Special") "Physical" else "Special")
                                else -> slot.copy(secretCategory = if (slot.secretCategory == "Special") "Physical" else "Special")
                            }
                            viewModel.updatePartySlot(updated)
                        },
                        onOpenDetail = { if (mon != null) viewModel.openDetailModal(mon) }
                    )
                }
            }

            // 2. PANEL A: "HOW YOUR TEAM TAKES DAMAGE" (TIERED THREAT & SHIELD RADAR)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Luxury Header with Circular Cyan Shield Icon + Defense Score Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonBlue.copy(alpha = 0.14f))
                                        .border(1.2.dp, NeonBlue.copy(alpha = 0.7f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "How Your Team Takes Damage",
                                        tint = NeonBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "How Your Team Takes Damage",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Team defensive vulnerabilities & resistances based on Pokémon types",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Defense Score Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(defenseGradePair.second.copy(alpha = 0.15f))
                                    .border(1.dp, defenseGradePair.second, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = defenseGradePair.first,
                                    color = defenseGradePair.second,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (stackedWeaknesses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF450A0A).copy(alpha = 0.65f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(9.dp)
                            ) {
                                Text(
                                    text = "⚠ Critical Stacked Weakness (3+ members weak): " +
                                            stackedWeaknesses.joinToString { "${it.type.displayName} (${it.totalWeak} weak)" },
                                    color = Color(0xFFFECACA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Selected Type Audit Inspection Drawer
                        selectedAuditTypeDefense?.let { auditedType ->
                            Spacer(modifier = Modifier.height(10.dp))
                            val w4List = activeMembers.filter { mon ->
                                PokemonType.getDefendingMultiplier(auditedType, mon.primaryType, mon.secondaryType, isModern) >= 4.0f
                            }
                            val w2List = activeMembers.filter { mon ->
                                val mult = PokemonType.getDefendingMultiplier(auditedType, mon.primaryType, mon.secondaryType, isModern)
                                mult >= 2.0f && mult < 4.0f
                            }
                            val resistList = activeMembers.filter { mon ->
                                val mult = PokemonType.getDefendingMultiplier(auditedType, mon.primaryType, mon.secondaryType, isModern)
                                mult > 0f && mult < 1.0f
                            }
                            val immuneList = activeMembers.filter { mon ->
                                PokemonType.getDefendingMultiplier(auditedType, mon.primaryType, mon.secondaryType, isModern) == 0f
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, auditedType.color.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TypePill(auditedType)
                                            Text(
                                                text = "Defensive Impact Audit",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Tap again to close",
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 3.dp))

                                    if (w4List.isNotEmpty()) {
                                        Text(
                                            text = "• 4× Double Weak: ${w4List.joinToString { it.name }}",
                                            color = Color(0xFFEF4444),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (w2List.isNotEmpty()) {
                                        Text(
                                            text = "• 2× Weak: ${w2List.joinToString { it.name }}",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (resistList.isNotEmpty()) {
                                        Text(
                                            text = "• Resistant (0.5× / 0.25×): ${resistList.joinToString { it.name }}",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (immuneList.isNotEmpty()) {
                                        Text(
                                            text = "• Immune (0×): ${immuneList.joinToString { it.name }}",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ZONE 1: DANGER ZONES / MAJOR THREATS
                        Text(
                            text = "ZONE 1 — DANGER ZONES & THREAT RADAR",
                            color = Color(0xFFF87171),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (activeMembers.isEmpty()) {
                            Text(
                                text = "Assign Pokémon to your party slots above to analyze team threats and shields.",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp
                            )
                        } else if (dangerThreats.isEmpty()) {
                            Text(
                                text = "No defensive weaknesses detected.",
                                color = NeonEmerald,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dangerThreats.forEach { stat ->
                                    val isSelected = selectedAuditTypeDefense == stat.type
                                    val weakMons = activeSlotsWithMon.mapNotNull { (slot, mon) ->
                                        val mult = PokemonType.getDefendingMultiplier(stat.type, mon.primaryType, mon.secondaryType, isModern)
                                        if (mult >= 2.0f) Triple(slot, mon, mult) else null
                                    }
                                    val isShielded = stat.resistOrImmuneCount > 0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0B0F19))
                                            .border(
                                                1.dp,
                                                when {
                                                    isSelected -> NeonBlue
                                                    stat.totalWeak >= 3 -> Color(0xFFEF4444)
                                                    !isShielded -> Color(0xFFF97316).copy(alpha = 0.7f)
                                                    else -> DarkCardBorder
                                                },
                                                RoundedCornerShape(9.dp)
                                            )
                                            .clickable {
                                                selectedAuditTypeDefense = if (isSelected) null else stat.type
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Enemy Attacking Type Pill
                                        Box(modifier = Modifier.width(68.dp)) {
                                            TypePill(stat.type)
                                        }

                                        // Middle: Mini-Sprites of Weak Team Members with 4x / 2x Tag
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState())
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            weakMons.forEach { (slot, mon, mult) ->
                                                val is4x = mult >= 4.0f
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (is4x) Color(0xFF7F1D1D).copy(alpha = 0.55f)
                                                            else Color(0xFF7C2D12).copy(alpha = 0.45f)
                                                        )
                                                        .border(
                                                            0.8.dp,
                                                            if (is4x) Color(0xFFEF4444) else Color(0xFFF97316),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = mon.spriteUrl(slot.isShiny),
                                                        contentDescription = mon.name,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Text(
                                                        text = if (is4x) "4×" else "2×",
                                                        color = if (is4x) Color(0xFFFCA5A5) else Color(0xFFFDBA74),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }

                                        // Right: Shielded vs Unshielded Indicator
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isShielded) NeonEmerald.copy(alpha = 0.14f)
                                                    else Color(0xFF7F1D1D).copy(alpha = 0.4f)
                                                )
                                                .border(
                                                    0.8.dp,
                                                    if (isShielded) NeonEmerald.copy(alpha = 0.7f) else Color(0xFFEF4444),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isShielded) "✓ ${stat.resistOrImmuneCount} Shield" else "⚠ Unshielded",
                                                color = if (isShielded) NeonEmerald else Color(0xFFFCA5A5),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ZONE 2: TEAM SHIELDS & IMMUNITIES
                        if (shieldTypes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "ZONE 2 — TEAM SHIELDS & IMMUNITIES (0.5× / 0.25× / 0×)",
                                color = NeonEmerald,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                shieldTypes.forEach { stat ->
                                    val isSelected = selectedAuditTypeDefense == stat.type
                                    val shieldMons = activeSlotsWithMon.mapNotNull { (slot, mon) ->
                                        val mult = PokemonType.getDefendingMultiplier(stat.type, mon.primaryType, mon.secondaryType, isModern)
                                        if (mult < 1.0f) Triple(slot, mon, mult) else null
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0B0F19))
                                            .border(
                                                1.dp,
                                                if (isSelected) NeonBlue else NeonEmerald.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedAuditTypeDefense = if (isSelected) null else stat.type
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.width(68.dp)) {
                                            TypePill(stat.type)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .horizontalScroll(rememberScrollState())
                                                .padding(start = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            shieldMons.forEach { (slot, mon, mult) ->
                                                val badgeTxt = when (mult) {
                                                    0f -> "0×"
                                                    0.25f -> "¼×"
                                                    else -> "½×"
                                                }
                                                val badgeCol = if (mult == 0f) Color(0xFF38BDF8) else NeonEmerald
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badgeCol.copy(alpha = 0.14f))
                                                        .border(0.8.dp, badgeCol.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = mon.spriteUrl(slot.isShiny),
                                                        contentDescription = mon.name,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = badgeTxt,
                                                        color = badgeCol,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Black,
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

                        // ZONE 3: NEUTRAL MATCHUPS
                        if (neutralTypes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ZONE 3 — PURE NEUTRAL DAMAGE (1× ACROSS TEAM)",
                                color = Color(0xFF94A3B8),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                neutralTypes.forEach { t ->
                                    TypePill(t)
                                }
                            }
                        }
                    }
                }
            }

            // 3. PANEL B: "WHAT YOUR TEAM CAN HIT HARD" (TACTICAL STRIKE & COVERAGE MATRIX)
            item {
                val totalMoves = (coverageBreakdown.physicalCount + coverageBreakdown.specialCount).coerceAtLeast(1)
                val physRatio = coverageBreakdown.physicalCount.toFloat() / totalMoves
                val physPct = (physRatio * 100).toInt()
                val specPct = 100 - physPct

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Luxury Header with Circular Gold Sword Icon + X/18 Types Covered Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonAmber.copy(alpha = 0.15f))
                                        .border(1.2.dp, NeonAmber.copy(alpha = 0.75f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Crossed Swords Vector Icon
                                    Canvas(modifier = Modifier.size(18.dp)) {
                                        val w = size.width
                                        val h = size.height
                                        drawLine(
                                            color = NeonAmber,
                                            start = Offset(w * 0.18f, h * 0.82f),
                                            end = Offset(w * 0.85f, h * 0.15f),
                                            strokeWidth = 2.4.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = NeonAmber,
                                            start = Offset(w * 0.22f, h * 0.56f),
                                            end = Offset(w * 0.44f, h * 0.78f),
                                            strokeWidth = 2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = NeonAmber,
                                            start = Offset(w * 0.82f, h * 0.82f),
                                            end = Offset(w * 0.15f, h * 0.15f),
                                            strokeWidth = 2.4.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                        drawLine(
                                            color = NeonAmber,
                                            start = Offset(w * 0.56f, h * 0.78f),
                                            end = Offset(w * 0.78f, h * 0.56f),
                                            strokeWidth = 2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "What Your Team Can Hit Hard",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "Super-effective offensive coverage from moves & secret types",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonEmerald.copy(alpha = 0.15f))
                                    .border(1.dp, NeonEmerald, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${coverageBreakdown.coveredTypes.size} / ${activeTypes.size} Types Covered",
                                    color = NeonEmerald,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bi-Directional Luxury Physical vs Special Balance Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚔ Physical: ${coverageBreakdown.physicalCount} ($physPct%)",
                                color = Color(0xFFF97316),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "✦ Special: ${coverageBreakdown.specialCount} ($specPct%)",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0B0F19))
                                .border(0.5.dp, DarkCardBorder, RoundedCornerShape(4.dp))
                        ) {
                            val safePhysWeight = if (activeMembers.isEmpty()) 0.5f else physRatio.coerceIn(0.05f, 0.95f)
                            Box(
                                modifier = Modifier
                                    .weight(safePhysWeight)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFEA580C), Color(0xFFF97316))
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f - safePhysWeight)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ZONE 1: UNCOVERED BLIND SPOTS (AT TOP OF PANEL B)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (coverageBreakdown.blindSpots.isEmpty()) Color(0xFF064E3B).copy(alpha = 0.35f)
                                    else Color(0xFF450A0A).copy(alpha = 0.55f)
                                )
                                .border(
                                    1.dp,
                                    if (coverageBreakdown.blindSpots.isEmpty()) NeonEmerald.copy(alpha = 0.7f)
                                    else Color(0xFFEF4444).copy(alpha = 0.8f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (coverageBreakdown.blindSpots.isEmpty()) {
                                        "✓ ZERO BLIND SPOTS — 100% SUPER-EFFECTIVE COVERAGE"
                                    } else {
                                        "⚠ ZONE 1 — UNCOVERED BLIND SPOTS (${coverageBreakdown.blindSpots.size} TYPES CANNOT BE HIT FOR 2×)"
                                    },
                                    color = if (coverageBreakdown.blindSpots.isEmpty()) NeonEmerald else Color(0xFFFCA5A5),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                if (coverageBreakdown.blindSpots.isEmpty()) {
                                    Text(
                                        text = "Every single elemental type in the chart is hit for 2× Super Effective damage by at least one team member.",
                                        color = Color(0xFFD1FAE5),
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        coverageBreakdown.blindSpots.forEach { t ->
                                            TypePill(t)
                                        }
                                    }

                                    if (recommendedMovesForBlindSpots.isNotEmpty()) {
                                        val recText = recommendedMovesForBlindSpots.joinToString(" or ") { atkType ->
                                            atkType.displayName
                                        }
                                        Text(
                                            text = "💡 Tactical Fix: Add a $recText move or Secret Type to patch these blind spots.",
                                            color = NeonAmber,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Selected Target Type Drill-Down Drawer (Shows Mini-Sprites + Move/Secret Type)
                        selectedAuditTypeOffense?.let { auditedType ->
                            Spacer(modifier = Modifier.height(10.dp))
                            val hittingEntries = mutableListOf<Triple<PartySlot, Pokemon, List<String>>>()
                            activeSlotsWithMon.forEach { (slot, mon) ->
                                val movePairs = buildList {
                                    add(Triple("M1", slot.move1Type, slot.move1Category))
                                    add(Triple("M2", slot.move2Type, slot.move2Category))
                                    add(Triple("M3", slot.move3Type, slot.move3Category))
                                    add(Triple("M4", slot.move4Type, slot.move4Category))
                                    slot.secretType?.let { st ->
                                        add(Triple("Secret", st, slot.secretCategory))
                                    }
                                }
                                val hittingMoves = movePairs.filter { (_, atkType, _) ->
                                    PokemonType.getMultiplier(atkType, auditedType, isModern) >= 2.0f
                                }.map { (slotLbl, atkType, cat) ->
                                    "$slotLbl: ${atkType.displayName} ($cat)"
                                }
                                if (hittingMoves.isNotEmpty()) {
                                    hittingEntries.add(Triple(slot, mon, hittingMoves))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, auditedType.color.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TypePill(auditedType)
                                            Text(
                                                text = "Team Members Hitting ${auditedType.displayName} (2×)",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Tap again to close",
                                            color = Color(0xFF64748B),
                                            fontSize = 9.sp
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFF1E293B))

                                    if (hittingEntries.isNotEmpty()) {
                                        hittingEntries.forEach { (slot, mon, moveDescs) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                AsyncImage(
                                                    model = mon.spriteUrl(slot.isShiny),
                                                    contentDescription = mon.name,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = mon.name,
                                                        color = Color.White,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text(
                                                        text = moveDescs.joinToString("  •  "),
                                                        color = NeonEmerald,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Blind Spot: 0/6 Pokémon carry a move or Secret Type that hits ${auditedType.displayName} for 2× damage.",
                                            color = Color(0xFFF87171),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ZONE 2: SUPER-EFFECTIVE COVERAGE MATRIX (BY TEAM MEMBERS 0/6..6/6 + 6-SEGMENT BAR)
                        Text(
                            text = "ZONE 2 — SUPER-EFFECTIVE COVERAGE MATRIX (TAP TYPE TO INSPECT)",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val covRows = activeTypes.chunked(2)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            covRows.forEach { rTypes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rTypes.forEach { t ->
                                        val memberHits = (coverageBreakdown.typeHitCounts[t] ?: 0).coerceIn(0, 6)
                                        val isSelected = selectedAuditTypeOffense == t
                                        val statusColor = when {
                                            memberHits >= 3 -> NeonEmerald
                                            memberHits in 1..2 -> NeonAmber
                                            else -> Color(0xFFEF4444)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0B0F19))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NeonAmber else statusColor.copy(alpha = 0.45f),
                                                    RoundedCornerShape(9.dp)
                                                )
                                                .clickable {
                                                    selectedAuditTypeOffense = if (isSelected) null else t
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TypePill(t)
                                                Text(
                                                    text = "$memberHits/6",
                                                    color = statusColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            // 6-Segment Mini Power Bar
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                for (seg in 1..6) {
                                                    val lit = seg <= memberHits
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(
                                                                if (lit) statusColor else Color(0xFF1E293B)
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(2 - rTypes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Pokémon Picker for Slot
    pokemonPickerSlotIndex?.let { targetSlotIdx ->
        var query by remember { mutableStateOf("") }
        var filterT by remember { mutableStateOf<PokemonType?>(null) }

        val filteredMons = remember(query, filterT) {
            viewModel.allPokemonList.filter { mon ->
                val matchQ = query.isBlank() || mon.name.lowercase().contains(query.lowercase()) || mon.id.toString() == query.trim()
                val matchT = filterT == null || mon.primaryType == filterT || mon.secondaryType == filterT
                matchQ && matchT
            }.sortedByDescending { it.bst }
        }

        Dialog(onDismissRequest = { pokemonPickerSlotIndex = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, NeonBlue, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Pokémon for Slot #${targetSlotIdx + 1}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { pokemonPickerSlotIndex = null }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        placeholder = { Text("Search Pokémon name or #id...", color = Color(0xFF64748B), fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = Color.White)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        activeTypes.forEach { t ->
                            val isSel = filterT == t
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) t.color else DarkSurfaceCard)
                                    .border(1.dp, if (isSel) t.color else DarkCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { filterT = if (isSel) null else t }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(t.displayName, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

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
                                        viewModel.assignPokemonToPartySlot(targetSlotIdx, mon.id)
                                        pokemonPickerSlotIndex = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = mon.spriteUrl(false), contentDescription = null, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${mon.name} (${mon.formattedShortId()})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("BST ${mon.bst}  •  Atk ${mon.attack} / SpA ${mon.spAtk}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TypePill(mon.primaryType)
                                    mon.secondaryType?.let { TypePill(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 2: Elemental Type Picker for Move 1..4 OR Secret Type (moveIndex == 5)
    typePickerTarget?.let { (targetSlot, moveIndex) ->
        val isSecretPicker = moveIndex == 5
        val titleLabel = if (isSecretPicker) {
            "Select Secret Type (Hidden Power / Tera)"
        } else {
            "Select Move #$moveIndex Elemental Type"
        }
        Dialog(onDismissRequest = { typePickerTarget = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, if (isSecretPicker) NeonAmber else NeonBlue, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(titleLabel, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = { typePickerTarget = null }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isSecretPicker) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updatePartySlot(targetSlot.copy(secretType = null))
                                    typePickerTarget = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NONE (DISABLE SECRET TYPE)",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    val typeRows = activeTypes.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        typeRows.forEach { rTypes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rTypes.forEach { t ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(t.color)
                                            .clickable {
                                                val updated = when (moveIndex) {
                                                    1 -> targetSlot.copy(move1Type = t)
                                                    2 -> targetSlot.copy(move2Type = t)
                                                    3 -> targetSlot.copy(move3Type = t)
                                                    4 -> targetSlot.copy(move4Type = t)
                                                    else -> targetSlot.copy(secretType = t)
                                                }
                                                viewModel.updatePartySlot(updated)
                                                typePickerTarget = null
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t.displayName.uppercase(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                repeat(3 - rTypes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 3: Interactive Two-Way AI Tactical Coach Live Chat
    if (showAiChatModal) {
        var chatInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAiChatModal = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, NeonBlue, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Interactive AI Tactical Coach",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Live 2-Way Strategy Chat • Syncing ${activeMembers.size}/6 Party Slots",
                                color = NeonBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { showAiChatModal = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPrompts = listOf(
                            "Cek Blind Spot & Coverage",
                            "Audit Kelemahan Defensif",
                            "Rasio Physical vs Special",
                            "Rekomendasi Roster & Tipe"
                        )
                        quickPrompts.forEach { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceCard)
                                    .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { viewModel.sendAiCoachMessage(prompt) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(prompt, color = NeonBlue, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(aiChatMessages, key = { it.id }) { msg ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = msg.senderLabel,
                                    color = if (msg.isUser) NeonAmber else NeonBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (msg.isUser) NeonBlue else Color(0xFF1E293B))
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = msg.message,
                                        color = if (msg.isUser) Color(0xFF0B0F19) else Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (msg.isUser) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Ask about blind spots, weaknesses...", color = Color(0xFF64748B), fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedContainerColor = DarkSurfaceCard,
                                unfocusedContainerColor = DarkSurfaceCard
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp, color = Color.White)
                        )

                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank()) {
                                    viewModel.sendAiCoachMessage(chatInput)
                                    chatInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonBlue)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF0B0F19),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSixSlotOverviewGrid(
    partySlots: List<PartySlot>,
    pokeMap: Map<Int, Pokemon>,
    selectedSlotIndex: Int?,
    onSelectSlot: (Int) -> Unit,
    onPickPokemonForEmpty: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2×3 TACTICAL ROSTER OVERVIEW (TAP SLOT TO CONFIGURE)",
                    color = NeonAmber,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            val rows = partySlots.chunked(3)
            rows.forEach { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowSlots.forEach { slot ->
                        val mon = slot.pokemonId?.let { pokeMap[it] }
                        val isSelected = selectedSlotIndex == slot.slotIndex

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0B0F19))
                                .border(
                                    1.2.dp,
                                    if (isSelected) NeonAmber else if (mon != null) NeonBlue.copy(alpha = 0.45f) else DarkCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (mon == null) onPickPokemonForEmpty(slot.slotIndex)
                                    else onSelectSlot(slot.slotIndex)
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (mon != null) {
                                AsyncImage(
                                    model = mon.spriteUrl(slot.isShiny),
                                    contentDescription = mon.name,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = mon.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "BST ${mon.bst}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            } else {
                                Box(
                                    modifier = Modifier.height(54.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Slot #${slot.slotIndex + 1}",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
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
    }
}

@Composable
private fun PartySlotTacticalCard(
    slot: PartySlot,
    pokemon: Pokemon?,
    onToggleShiny: () -> Unit,
    onSelectPokemon: () -> Unit,
    onClearSlot: () -> Unit,
    onPickMoveType: (Int) -> Unit,
    onToggleMoveCategory: (Int) -> Unit,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (pokemon != null) NeonBlue.copy(alpha = 0.45f) else DarkCardBorder,
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLOT #${slot.slotIndex + 1}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Shiny toggle per slot (defaults to Normal, toggles to ✦ Shiny)
                    if (pokemon != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (slot.isShiny) NeonAmber.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                .border(1.dp, if (slot.isShiny) NeonAmber else Color(0xFF334155), RoundedCornerShape(6.dp))
                                .clickable { onToggleShiny() }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("btn_shiny_slot_${slot.slotIndex}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (slot.isShiny) "✦ Shiny" else "Normal",
                                color = if (slot.isShiny) NeonAmber else Color(0xFF94A3B8),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable { onSelectPokemon() }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (pokemon != null) "Change" else "+ Select Pokémon",
                            color = NeonBlue,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (pokemon != null) {
                        IconButton(
                            onClick = onClearSlot,
                            modifier = Modifier.size(22.dp)
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
            }

            if (pokemon != null) {
                Spacer(modifier = Modifier.height(6.dp))

                // Pokémon Identity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .clickable { onOpenDetail() },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = pokemon.spriteUrl(slot.isShiny),
                            contentDescription = pokemon.name,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = pokemon.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onOpenDetail() }
                            )
                            TypePill(pokemon.primaryType)
                            pokemon.secondaryType?.let { TypePill(it) }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "BST ${pokemon.bst}  •  Atk ${pokemon.attack}  •  Sp.Atk ${pokemon.spAtk}  •  Spe ${pokemon.speed}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4 Moves Configuration Grid (2x2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoveTypeCategorySlot(
                        label = "M1",
                        resolvedType = slot.move1Type,
                        category = slot.move1Category,
                        onPickType = { onPickMoveType(1) },
                        onToggleCategory = { onToggleMoveCategory(1) },
                        modifier = Modifier.weight(1f)
                    )
                    MoveTypeCategorySlot(
                        label = "M2",
                        resolvedType = slot.move2Type,
                        category = slot.move2Category,
                        onPickType = { onPickMoveType(2) },
                        onToggleCategory = { onToggleMoveCategory(2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MoveTypeCategorySlot(
                        label = "M3",
                        resolvedType = slot.move3Type,
                        category = slot.move3Category,
                        onPickType = { onPickMoveType(3) },
                        onToggleCategory = { onToggleMoveCategory(3) },
                        modifier = Modifier.weight(1f)
                    )
                    MoveTypeCategorySlot(
                        label = "M4",
                        resolvedType = slot.move4Type,
                        category = slot.move4Category,
                        onPickType = { onPickMoveType(4) },
                        onToggleCategory = { onToggleMoveCategory(4) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5th Row: Secret Type (Hidden Power / Tera) + [Phys | Spec] Toggle
                SecretTypeCategoryRow(
                    secretType = slot.secretType,
                    category = slot.secretCategory,
                    onPickSecretType = { onPickMoveType(5) },
                    onToggleCategory = { onToggleMoveCategory(5) }
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B0F19))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelectPokemon() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Tap to Assign Pokémon & Configure Moves",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveTypeCategorySlot(
    label: String,
    resolvedType: PokemonType,
    category: String,
    onPickType: () -> Unit,
    onToggleCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSpecial = category.equals("Special", ignoreCase = true)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B0F19))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(resolvedType.color)
                    .clickable(onClick = onPickType)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = resolvedType.displayName.uppercase(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (isSpecial) Color(0xFF0284C7).copy(alpha = 0.22f) else Color(0xFFEA580C).copy(alpha = 0.22f)
                )
                .border(
                    1.dp,
                    if (isSpecial) Color(0xFF38BDF8) else Color(0xFFF97316),
                    RoundedCornerShape(5.dp)
                )
                .clickable(onClick = onToggleCategory)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isSpecial) "Spec" else "Phys",
                color = if (isSpecial) Color(0xFF38BDF8) else Color(0xFFF97316),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun SecretTypeCategoryRow(
    secretType: PokemonType?,
    category: String,
    onPickSecretType: () -> Unit,
    onToggleCategory: () -> Unit
) {
    val isSpecial = category.equals("Special", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0B0F19))
            .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "✦ Secret Type (HP / Tera)",
                color = NeonAmber,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(secretType?.color ?: Color(0xFF1E293B))
                    .border(
                        0.8.dp,
                        secretType?.color ?: Color(0xFF475569),
                        RoundedCornerShape(5.dp)
                    )
                    .clickable(onClick = onPickSecretType)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = secretType?.displayName?.uppercase() ?: "NONE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(
                    if (isSpecial) Color(0xFF0284C7).copy(alpha = 0.22f) else Color(0xFFEA580C).copy(alpha = 0.22f)
                )
                .border(
                    1.dp,
                    if (isSpecial) Color(0xFF38BDF8) else Color(0xFFF97316),
                    RoundedCornerShape(5.dp)
                )
                .clickable(onClick = onToggleCategory)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isSpecial) "Spec" else "Phys",
                color = if (isSpecial) Color(0xFF38BDF8) else Color(0xFFF97316),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
