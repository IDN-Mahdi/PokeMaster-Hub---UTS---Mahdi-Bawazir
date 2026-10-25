package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BattleDrillsEngine
import com.example.data.DrillCategoryFilter
import com.example.data.DrillQuestionCategory
import com.example.data.model.PokemonType
import com.example.ui.PokemonViewModel
import com.example.ui.components.TypePill
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class MentorSection(val label: String) {
    CALCULATOR("Calculator"),
    PRACTICE("Practice"),
    ANALOGIES("Logic Analogies")
}

@Composable
fun Tab4PokeTypeMentorScreen(
    viewModel: PokemonViewModel,
    modifier: Modifier = Modifier
) {
    val isModern by viewModel.isModern.collectAsState()

    var activeSection by remember { mutableStateOf(MentorSection.CALCULATOR) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkScaffoldBg)
    ) {
        // Top Header with Top-Left Modern / Gen 4 Toggle + Section Selector
        Surface(
            color = DarkSurfaceHeader,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left Modern / Gen 4 Toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, NeonAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.toggleMechanicMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("mentor_chart_mode_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isModern) NeonEmerald else NeonAmber)
                        )
                        Text(
                            text = if (isModern) "Modern Chart (18T)" else "Gen 4 Chart (17T)",
                            color = if (isModern) NeonEmerald else NeonAmber,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = if (isModern) "18 Types • All Combos" else "17 Types • No Fairy",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Segmented Sub-Tab Navigation: Calculator | Practice | Logic Analogies
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MentorSection.entries.forEach { section ->
                        val isSel = activeSection == section
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonBlue else DarkSurfaceCard)
                                .border(
                                    1.dp,
                                    if (isSel) NeonBlue else DarkCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { activeSection = section }
                                .padding(vertical = 8.dp)
                                .testTag("mentor_tab_${section.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = section.label,
                                color = if (isSel) Color(0xFF0B0F19) else Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        when (activeSection) {
            MentorSection.CALCULATOR -> CleanDualTypeCalculator(isModern = isModern)
            MentorSection.PRACTICE -> CompleteBattleDrillsView(
                isModern = isModern,
                onAnswer = { viewModel.recordQuizAnswer(it) }
            )
            MentorSection.ANALOGIES -> SingleTypeLogicAnalogiesView(isModern = isModern)
        }
    }
}

// ---------------------------------------------------------------------------
// PART 1: DUAL TYPE CALCULATOR REDESIGN
// - 6x3 Grid showing all 18 types simultaneously
// - Selection order determines Primary (#1) vs Secondary (#2)
// - Removed Swap & Copy Profile buttons; added Clear/Reset button
// - Minimalist Centered Header Icons: Shield, Sword, Target/Crosshair
// ---------------------------------------------------------------------------
@Composable
fun CleanDualTypeCalculator(isModern: Boolean) {
    val activeTypes = remember(isModern) { PokemonType.activeTypes(isModern) }
    val gridTypes = BattleDrillsEngine.GRID_TYPES_ORDER

    // Ordered list of selected types (max 2): index 0 = Primary, index 1 = Secondary
    var selectedTypes by remember {
        mutableStateOf(listOf(PokemonType.WATER, PokemonType.GROUND))
    }
    var selectedSimAttackType by remember { mutableStateOf(PokemonType.GRASS) }
    var attackDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isModern) {
        if (!isModern) {
            val filtered = selectedTypes.filter { it != PokemonType.FAIRY }
            selectedTypes = filtered.ifEmpty { listOf(PokemonType.STEEL) }
            if (selectedSimAttackType == PokemonType.FAIRY) {
                selectedSimAttackType = PokemonType.ICE
            }
        }
    }

    val primaryDef = selectedTypes.firstOrNull() ?: PokemonType.NORMAL
    val secondaryDef = selectedTypes.getOrNull(1)

    // Check if current pairing is one of the 9 unused combinations
    val matchedCombo = remember(primaryDef, secondaryDef) {
        BattleDrillsEngine.ALL_171_COMBOS.firstOrNull { c ->
            (c.primary == primaryDef && c.secondary == secondaryDef) ||
                    (secondaryDef != null && c.primary == secondaryDef && c.secondary == primaryDef)
        }
    }

    // Defensive Multipliers grouped by 0x, 0.25x, 0.5x, 1x, 2x, 4x
    val weak4x = remember(primaryDef, secondaryDef, isModern) {
        activeTypes.filter { PokemonType.getDefendingMultiplier(it, primaryDef, secondaryDef, isModern) == 4.0f }
    }
    val weak2x = remember(primaryDef, secondaryDef, isModern) {
        activeTypes.filter { PokemonType.getDefendingMultiplier(it, primaryDef, secondaryDef, isModern) == 2.0f }
    }
    val neutral1x = remember(primaryDef, secondaryDef, isModern) {
        activeTypes.filter { PokemonType.getDefendingMultiplier(it, primaryDef, secondaryDef, isModern) == 1.0f }
    }
    val resist05x = remember(primaryDef, secondaryDef, isModern) {
        activeTypes.filter { PokemonType.getDefendingMultiplier(it, primaryDef, secondaryDef, isModern) == 0.5f }
    }
    val resist025x = remember(primaryDef, secondaryDef, isModern) {
        activeTypes.filter { PokemonType.getDefendingMultiplier(it, primaryDef, secondaryDef, isModern) == 0.25f }
    }
    val immune0x = remember(primaryDef, secondaryDef, isModern) {
        activeTypes.filter { PokemonType.getDefendingMultiplier(it, primaryDef, secondaryDef, isModern) == 0.0f }
    }

    // Offensive STAB Breakdown per individual selected type (NEVER merged into a single misleading list)
    val primaryStab2x = remember(primaryDef, isModern) {
        activeTypes.filter { PokemonType.getMultiplier(primaryDef, it, isModern) == 2.0f }
    }
    val primaryStab05x = remember(primaryDef, isModern) {
        activeTypes.filter { PokemonType.getMultiplier(primaryDef, it, isModern) == 0.5f }
    }
    val primaryStab0x = remember(primaryDef, isModern) {
        activeTypes.filter { PokemonType.getMultiplier(primaryDef, it, isModern) == 0.0f }
    }

    val secondaryStab2x = remember(secondaryDef, isModern) {
        secondaryDef?.let { sec -> activeTypes.filter { PokemonType.getMultiplier(sec, it, isModern) == 2.0f } } ?: emptyList()
    }
    val secondaryStab05x = remember(secondaryDef, isModern) {
        secondaryDef?.let { sec -> activeTypes.filter { PokemonType.getMultiplier(sec, it, isModern) == 0.5f } } ?: emptyList()
    }
    val secondaryStab0x = remember(secondaryDef, isModern) {
        secondaryDef?.let { sec -> activeTypes.filter { PokemonType.getMultiplier(sec, it, isModern) == 0.0f } } ?: emptyList()
    }

    val mult1 = PokemonType.getMultiplier(selectedSimAttackType, primaryDef, isModern)
    val mult2 = secondaryDef?.let { PokemonType.getMultiplier(selectedSimAttackType, it, isModern) } ?: 1.0f
    val finalMultiplier = mult1 * mult2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 6x3 TYPE SELECTION GRID CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Active Selection Status + Clear/Reset Button (No Swap or Copy Profile)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TypePill(primaryDef)
                        if (secondaryDef != null) {
                            TypePill(secondaryDef)
                        } else {
                            Text(
                                text = "SINGLE TYPE",
                                color = Color(0xFF64748B),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (matchedCombo?.isUnusedInMainSeries == true) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonAmber.copy(alpha = 0.18f))
                                    .border(1.dp, NeonAmber, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "UNUSED COMBO",
                                    color = NeonAmber,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Clear / Reset Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                selectedTypes = listOf(primaryDef)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("clear_type_selection_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Selection",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Clear / Reset",
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select up to 2 types from the 6×3 grid (1st click = Primary, 2nd click = Secondary)",
                    color = Color(0xFF64748B),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 6x3 Grid Table showing all 18 types simultaneously
                val rows = gridTypes.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowTypes.forEach { t ->
                                val isDisabledInGen4 = !isModern && t == PokemonType.FAIRY
                                val selIndex = selectedTypes.indexOf(t) // 0 = Primary, 1 = Secondary, -1 = unselected
                                val isSelected = selIndex != -1

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isDisabledInGen4 -> Color(0xFF0F172A).copy(alpha = 0.4f)
                                                isSelected -> t.color
                                                else -> Color(0xFF1F2937)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.8.dp else 1.dp,
                                            color = when {
                                                isDisabledInGen4 -> Color(0xFF334155).copy(alpha = 0.3f)
                                                selIndex == 0 -> Color.White
                                                selIndex == 1 -> NeonAmber
                                                else -> t.color.copy(alpha = 0.45f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = !isDisabledInGen4) {
                                            selectedTypes = when {
                                                selectedTypes.contains(t) -> {
                                                    if (selectedTypes.size > 1) {
                                                        selectedTypes.filter { it != t }
                                                    } else {
                                                        selectedTypes
                                                    }
                                                }
                                                selectedTypes.size < 2 -> selectedTypes + t
                                                else -> listOf(selectedTypes.first(), t)
                                            }
                                        }
                                        .padding(horizontal = 3.dp, vertical = 2.dp)
                                        .testTag("grid_type_${t.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = t.displayName.uppercase(),
                                            color = if (isDisabledInGen4) Color(0xFF475569) else Color.White,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = if (selIndex == 0) "1ST • PRI" else "2ND • SEC",
                                                color = if (selIndex == 0) Color(0xFF0B0F19) else Color.White,
                                                fontSize = 7.sp,
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
            }
        }

        // 1. DEFENSIVE DURABILITY SECTION ("How You Take Damage" + Circular Shield Icon)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CenteredMinimalistIconHeader(
                    iconType = HeaderIconKind.SHIELD,
                    tint = NeonBlue,
                    tooltip = "How You Take Damage • Defensive Durability"
                )
                CleanMultiplierBlock("4×", "Double Weak", weak4x, Color(0xFF7F1D1D), Color(0xFFEF4444))
                CleanMultiplierBlock("2×", "Weakness", weak2x, Color(0xFF7C2D12), Color(0xFFF97316))
                CleanMultiplierBlock("1×", "Neutral", neutral1x, Color(0xFF1E293B), Color(0xFF38BDF8))
                CleanMultiplierBlock("0.5×", "Resistance", resist05x, Color(0xFF064E3B), Color(0xFF10B981))
                CleanMultiplierBlock("0.25×", "Double Resist", resist025x, Color(0xFF065F46), Color(0xFF34D399))
                CleanMultiplierBlock("0×", "Immunity", immune0x, Color(0xFF0F172A), Color(0xFF94A3B8))
            }
        }

        // 2. OFFENSIVE COVERAGE SECTION ("What You Can Hit Hard" — SPLIT BY INDIVIDUAL STAB TYPE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CenteredMinimalistIconHeader(
                    iconType = HeaderIconKind.SWORD,
                    tint = NeonAmber,
                    tooltip = "What You Can Hit Hard • Offensive STAB Coverage"
                )

                // Panel 1: Primary Type STAB Moves
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B0F19))
                        .border(1.dp, primaryDef.color.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TypePill(primaryDef)
                        Text(
                            text = "${primaryDef.displayName.uppercase()} STAB MOVES (PRIMARY)",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    CleanMultiplierBlock("2×", "Super Effective", primaryStab2x, Color(0xFF78350F), NeonAmber)
                    CleanMultiplierBlock("0.5×", "Not Effective", primaryStab05x, Color(0xFF1E293B), Color(0xFFCBD5E1))
                    if (primaryStab0x.isNotEmpty()) {
                        CleanMultiplierBlock("0×", "No Effect", primaryStab0x, Color(0xFF0F172A), Color(0xFF94A3B8))
                    }
                }

                // Panel 2: Secondary Type STAB Moves (Only shown when 2 types are selected)
                if (secondaryDef != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, secondaryDef.color.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TypePill(secondaryDef)
                            Text(
                                text = "${secondaryDef.displayName.uppercase()} STAB MOVES (SECONDARY)",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        CleanMultiplierBlock("2×", "Super Effective", secondaryStab2x, Color(0xFF78350F), NeonAmber)
                        CleanMultiplierBlock("0.5×", "Not Effective", secondaryStab05x, Color(0xFF1E293B), Color(0xFFCBD5E1))
                        if (secondaryStab0x.isNotEmpty()) {
                            CleanMultiplierBlock("0×", "No Effect", secondaryStab0x, Color(0xFF0F172A), Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        // 3. SINGLE ATTACK MULTIPLIER CALCULATOR (Centered Target/Crosshair Icon Header)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                CenteredMinimalistIconHeader(
                    iconType = HeaderIconKind.TARGET,
                    tint = NeonEmerald,
                    tooltip = "Single Attack Multiplier Calculator"
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown + Quick Selector for Specific Attack Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attacking Type:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, selectedSimAttackType.color, RoundedCornerShape(8.dp))
                                .clickable { attackDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("single_attack_dropdown"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TypePill(selectedSimAttackType)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Attack Type",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = attackDropdownExpanded,
                            onDismissRequest = { attackDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF0F172A))
                        ) {
                            activeTypes.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TypePill(t)
                                            Text(t.displayName, color = Color.White, fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedSimAttackType = t
                                        attackDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${BattleDrillsEngine.formatMult(mult1)}× (${selectedSimAttackType.displayName} → ${primaryDef.displayName}) × ${BattleDrillsEngine.formatMult(mult2)}× (${selectedSimAttackType.displayName} → ${secondaryDef?.displayName ?: "None"}) = ${BattleDrillsEngine.formatMult(finalMultiplier)}× Damage",
                        color = when (finalMultiplier) {
                            4.0f -> Color(0xFFEF4444)
                            2.0f -> Color(0xFFF97316)
                            1.0f -> Color(0xFF38BDF8)
                            0.5f -> Color(0xFF10B981)
                            0.25f -> Color(0xFF34D399)
                            else -> Color(0xFF94A3B8)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private enum class HeaderIconKind {
    SHIELD,
    SWORD,
    TARGET
}

@Composable
private fun CenteredMinimalistIconHeader(
    iconType: HeaderIconKind,
    tint: Color,
    tooltip: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f))
                .border(1.2.dp, tint.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (iconType) {
                HeaderIconKind.SHIELD -> {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = tooltip,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                HeaderIconKind.SWORD -> {
                    // Custom minimalist crossed-swords vector drawing
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        // Blade 1 (bottom-left to top-right)
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.18f, h * 0.82f),
                            end = Offset(w * 0.85f, h * 0.15f),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Crossguard 1
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.22f, h * 0.56f),
                            end = Offset(w * 0.44f, h * 0.78f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Blade 2 (bottom-right to top-left)
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.82f, h * 0.82f),
                            end = Offset(w * 0.15f, h * 0.15f),
                            strokeWidth = 2.4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        // Crossguard 2
                        drawLine(
                            color = tint,
                            start = Offset(w * 0.56f, h * 0.78f),
                            end = Offset(w * 0.78f, h * 0.56f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                HeaderIconKind.TARGET -> {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = tooltip,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tooltip.uppercase(),
            color = tint.copy(alpha = 0.85f),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun CleanMultiplierBlock(
    badgeText: String,
    label: String,
    types: List<PokemonType>,
    containerColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor.copy(alpha = 0.25f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor.copy(alpha = 0.2f))
                .border(1.dp, accentColor, RoundedCornerShape(4.dp))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = accentColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(92.dp)
        )

        if (types.isEmpty()) {
            Text("None", color = Color(0xFF64748B), fontSize = 10.sp)
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                types.forEach { t ->
                    TypePill(t)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PART 2: PRACTICE (ADVANTAGE, STAB, & RESISTANCE DRILLS)
// - No Timer, Accuracy, Streak, or Score clutter
// - Gen 4 mode automatically filters out all Fairy-type questions
// - Advantage: 3 plain-text options (Player Advantage, Equal, Opponent Advantage)
// - STAB: Colorful TypePill panels ONLY (no duplicate plain text)
// - Resistance: Neutral phrasing + Colorful TypePill panels ONLY + Black pill panel for None
// ---------------------------------------------------------------------------
@Composable
fun CompleteBattleDrillsView(
    isModern: Boolean,
    onAnswer: (Boolean) -> Unit
) {
    var categoryFilter by remember { mutableStateOf(DrillCategoryFilter.ALL) }
    var poolPosition by remember { mutableStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var feedbackState by remember { mutableStateOf<Boolean?>(null) }

    val validGlobalIndices = remember(categoryFilter, isModern) {
        BattleDrillsEngine.getValidGlobalIndices(categoryFilter, isModern)
    }

    fun selectPoolPosition(newPos: Int) {
        if (validGlobalIndices.isEmpty()) return
        val wrapped = ((newPos % validGlobalIndices.size) + validGlobalIndices.size) % validGlobalIndices.size
        poolPosition = wrapped
        selectedOptionIndex = null
        feedbackState = null
    }

    fun jumpRandomInCategory() {
        if (validGlobalIndices.isEmpty()) return
        selectPoolPosition(validGlobalIndices.indices.random())
    }

    LaunchedEffect(categoryFilter, isModern) {
        poolPosition = 0
        selectedOptionIndex = null
        feedbackState = null
    }

    val safePoolPos = poolPosition.coerceIn(0, (validGlobalIndices.size - 1).coerceAtLeast(0))
    val currentGlobalIndex = validGlobalIndices.getOrElse(safePoolPos) { 0 }

    val currentQuestion = remember(currentGlobalIndex, isModern) {
        BattleDrillsEngine.generateQuestionByGlobalIndex(currentGlobalIndex, isModern)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Category Filter Row: All | Advantage | STAB | Resistance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DrillCategoryFilter.entries.forEach { filter ->
                val isSel = categoryFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) NeonBlue.copy(alpha = 0.22f) else DarkSurfaceCard)
                        .border(1.dp, if (isSel) NeonBlue else DarkCardBorder, RoundedCornerShape(8.dp))
                        .clickable { categoryFilter = filter }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("drill_filter_${filter.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSel) NeonBlue else Color(0xFF94A3B8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Question Counter & Navigation Bar ("Question X/Y" without redundant subtitle underneath)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { selectPoolPosition(safePoolPos - 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Question",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Question ${safePoolPos + 1}/${validGlobalIndices.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )

                    IconButton(
                        onClick = { selectPoolPosition(safePoolPos + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Question",
                            tint = Color.White
                        )
                    }
                }

                // Random Question Button (Always single line, never wraps vertically)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, NeonAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable { jumpRandomInCategory() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("random_drill_question_button")
                ) {
                    Text(
                        text = "Random Drill",
                        color = NeonAmber,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Question Prompt Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Visual Type Matchup Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B0F19))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentQuestion.playerCombo != null) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "YOUR POKÉMON",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentQuestion.playerCombo.isUnusedInMainSeries) {
                                    Text(
                                        text = "• UNUSED COMBO",
                                        color = NeonAmber,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TypePill(currentQuestion.playerCombo.primary)
                                currentQuestion.playerCombo.secondary?.let { TypePill(it) }
                            }
                        }
                    }

                    if (currentQuestion.playerCombo != null && currentQuestion.opponentCombo != null) {
                        Text(
                            text = "VS",
                            color = NeonAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (currentQuestion.opponentCombo != null) {
                        Column(
                            horizontalAlignment = if (currentQuestion.playerCombo != null) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "OPPONENT TYPE",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentQuestion.opponentCombo.isUnusedInMainSeries) {
                                    Text(
                                        text = "• UNUSED COMBO",
                                        color = NeonAmber,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TypePill(currentQuestion.opponentCombo.primary)
                                currentQuestion.opponentCombo.secondary?.let { TypePill(it) }
                            }
                        }
                    }

                    if (currentQuestion.neutralCombo != null) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "POKÉMON TYPE",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentQuestion.neutralCombo.isUnusedInMainSeries) {
                                    Text(
                                        text = "• UNUSED COMBO",
                                        color = NeonAmber,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TypePill(currentQuestion.neutralCombo.primary)
                                currentQuestion.neutralCombo.secondary?.let { TypePill(it) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentQuestion.prompt,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Answer Options
        // - ADVANTAGE: Plain text ONLY (no colorful TypePill panels)
        // - STAB & RESISTANCE: Colorful TypePill panels ONLY (no duplicate plain text),
        //   and for None ("None (No 0× Immunities)" / etc.), render inside a black pill panel!
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currentQuestion.options.forEachIndexed { idx, optLabel ->
                val isSelected = selectedOptionIndex == idx
                val isCorrect = idx == currentQuestion.correctIndex
                val optTypes = currentQuestion.optionTypes.getOrNull(idx).orEmpty()

                val (bgColor, borderCol, textColor) = when {
                    feedbackState != null && isCorrect -> Triple(Color(0xFF064E3B), Color(0xFF10B981), Color(0xFF34D399))
                    feedbackState != null && isSelected && !isCorrect -> Triple(Color(0xFF7F1D1D), Color(0xFFEF4444), Color(0xFFFCA5A5))
                    else -> Triple(DarkSurfaceCard, DarkCardBorder, Color.White)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                        .clickable(enabled = feedbackState == null) {
                            selectedOptionIndex = idx
                            feedbackState = isCorrect
                            onAnswer(isCorrect)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("drill_option_$idx"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (currentQuestion.category == DrillQuestionCategory.ADVANTAGE) {
                            // Advantage: Plain text ONLY, no colorful panels
                            Text(
                                text = optLabel,
                                color = textColor,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // STAB & Resistance: Colorful TypePill panels ONLY (no duplicate plain text)
                            if (optTypes.isNotEmpty()) {
                                optTypes.forEach { t ->
                                    TypePill(t)
                                }
                            } else {
                                // Black Pill Panel for "None (No 0× Immunities)" / "None"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF05070B))
                                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = optLabel.uppercase(),
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    if (feedbackState != null && isCorrect) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Correct",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (feedbackState != null && isSelected && !isCorrect) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Incorrect",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Post-Answer Explanation & Next Question Controls
        if (feedbackState != null) {
            val correct = feedbackState!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (correct) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFF450A0A).copy(alpha = 0.45f))
                    .border(1.dp, if (correct) Color(0xFF10B981) else Color(0xFFEF4444), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (correct) "CORRECT ANSWER" else "INCORRECT ANSWER",
                    color = if (correct) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentQuestion.calculationBreakdown,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentQuestion.explanation,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectPoolPosition(safePoolPos + 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color(0xFF0B0F19)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Text("Next Question", fontWeight = FontWeight.ExtraBold)
                }

                Button(
                    onClick = { jumpRandomInCategory() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = NeonAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Random Drill", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// MODE 3: SINGLE-TYPE LOGIC ANALOGIES (SELECT 1 TYPE -> OFFENSIVE & DEFENSIVE DEEP DIVE, NO SEARCH BAR)
// ---------------------------------------------------------------------------
data class TypeLogicGuide(
    val type: PokemonType,
    val archetypeTitle: String,
    val coreConcept: String,
    val offensiveLogic: List<Pair<PokemonType, String>>,
    val defensiveWeakLogic: List<Pair<PokemonType, String>>,
    val defensiveResistLogic: List<String>
)

private val typeLogicMap: Map<PokemonType, TypeLogicGuide> = mapOf(
    PokemonType.NORMAL to TypeLogicGuide(
        type = PokemonType.NORMAL,
        archetypeTitle = "Standard Physical & Biological Baseline",
        coreConcept = "Melambangkan makhluk hidup biasa tanpa manipulasi elemen alam maupun energi supranatural.",
        offensiveLogic = emptyList(),
        defensiveWeakLogic = listOf(
            PokemonType.FIGHTING to "Teknik bela diri terlatih (Fighting) mampu melumpuhkan pertahanan fisik biasa secara telak."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Ghost: Energi arwah/dimensi astral dan tubuh fisik biasa berada di frekuensi yang tidak saling bersentuhan.",
            "0.5x Ditahan oleh Rock & Steel: Pukulan fisik biasa tidak efektif menghancurkan batu keras maupun baja tempa."
        )
    ),
    PokemonType.FIRE to TypeLogicGuide(
        type = PokemonType.FIRE,
        archetypeTitle = "Thermal Combustion & High-Entropy Energy",
        coreConcept = "Energi panas ekstrem yang membakar materi organik, mencairkan pembekuan, dan melunakkan logam.",
        offensiveLogic = listOf(
            PokemonType.GRASS to "Selulosa tumbuhan kering langsung terbakar habis oleh kobaran api.",
            PokemonType.ICE to "Struktur kristal es mencair seketika saat terkena suhu tinggi.",
            PokemonType.BUG to "Eksoskeleton kitin serangga sangat rentan terhadap dehidrasi dan panas ekstrem.",
            PokemonType.STEEL to "Logam baja akan memuai dan meleleh di dalam tungku pembakaran bersuhu tinggi."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.WATER to "Air menyerap panas secara drastis dan memutus suplai oksigen api.",
            PokemonType.GROUND to "Pasir dan tanah menimbun oksigen sehingga mematikan nyala api.",
            PokemonType.ROCK to "Batu vulkanik dan mineral padat tahan panas serta mampu memadamkan bara api."
        ),
        defensiveResistLogic = listOf(
            "Menahan Fire, Grass, Ice, Bug, Steel, dan Fairy berkat pelindung suhu panas tinggi yang menguapkan serangan organik/dingin."
        )
    ),
    PokemonType.WATER to TypeLogicGuide(
        type = PokemonType.WATER,
        archetypeTitle = "Fluid Hydro-Dynamics & Thermal Absorption",
        coreConcept = "Fluida adaptif yang memadamkan panas, mengikis batuan, dan melarutkan mineral tanah.",
        offensiveLogic = listOf(
            PokemonType.FIRE to "Air memutus reaksi oksidasi pembakaran dan menurunkan suhu secara instan.",
            PokemonType.GROUND to "Aliran air deras menyebabkan erosi tanah dan mengubah tanah kering menjadi lumpur.",
            PokemonType.ROCK to "Tekanan hidrolik air mengikis dan memecah struktur batuan mineral."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.ELECTRIC to "Air alami mengandung elektrolit dan mineral terlarut yang menghantarkan arus listrik tegangan tinggi.",
            PokemonType.GRASS to "Sistem akar tumbuhan menyerap air sebagai nutrisi utama pertumbuhan."
        ),
        defensiveResistLogic = listOf(
            "Menahan Fire, Water, Ice, dan Steel karena kapasitas kalor air yang tinggi serta sifat cair yang tidak bisa dipotong logam."
        )
    ),
    PokemonType.GRASS to TypeLogicGuide(
        type = PokemonType.GRASS,
        archetypeTitle = "Botanical Life & Root Absorption",
        coreConcept = "Organisme fotosintesis yang menyerap air serta mineral bumi dan memecah batuan dengan akar.",
        offensiveLogic = listOf(
            PokemonType.WATER to "Tumbuhan menyerap cadangan air hingga kering melalui jaringan kapiler akar.",
            PokemonType.GROUND to "Akar tanaman menembus dan mengisap unsur hara dari lapisan tanah.",
            PokemonType.ROCK to "Pelapukan biologis: akar pohon yang tumbuh mampu membelah batu karang yang keras."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.FIRE to "Jaringan tanaman mudah terbakar.",
            PokemonType.ICE to "Suhu beku merusak dinding sel tumbuhan (frostbite).",
            PokemonType.POISON to "Herbisida dan racun kimia mematikan metabolisme flora.",
            PokemonType.FLYING to "Burung mematuk biji-bijian dan daun tanaman dari udara.",
            PokemonType.BUG to "Serangga herbivora memakan dedaunan dan batang tanaman."
        ),
        defensiveResistLogic = listOf(
            "Menahan Water, Electric (kayu/serat organik adalah isolator listrik), Grass, dan Ground."
        )
    ),
    PokemonType.ELECTRIC to TypeLogicGuide(
        type = PokemonType.ELECTRIC,
        archetypeTitle = "High-Voltage Electromagnetic Discharge",
        coreConcept = "Arus elektron berkecepatan tinggi yang menyambar konduktor air dan objek di angkasa.",
        offensiveLogic = listOf(
            PokemonType.WATER to "Air bermuatan ion menjadi konduktor sempurna bagi sengatan listrik.",
            PokemonType.FLYING to "Objek yang terbang tinggi di langit terbuka menjadi sasaran utama sambaran petir tanpa grounding."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.GROUND to "Bumi berfungsi sebagai penetral arus listrik alami (grounding) yang menyerap tegangan ke dalam tanah."
        ),
        defensiveResistLogic = listOf(
            "Menahan Electric, Flying, dan Steel (induksi elektromagnetik pada konduktor logam)."
        )
    ),
    PokemonType.ICE to TypeLogicGuide(
        type = PokemonType.ICE,
        archetypeTitle = "Cryogenic Freezing & Brittle Crystal",
        coreConcept = "Tipe ofensif premier (Glass Cannon) yang membekukan ekosistem udara, tanah, flora, dan reptil naga.",
        offensiveLogic = listOf(
            PokemonType.GRASS to "Membekukan cairan sel tumbuhan hingga mati.",
            PokemonType.GROUND to "Fenomena frost heave: air yang membeku di pori-pori tanah memuai dan meretakkan daratan.",
            PokemonType.FLYING to "Pembekuan sayap dan arus udara dingin menjatuhkan burung dari angkasa.",
            PokemonType.DRAGON to "Naga sebagai reptil mistis berdarah dingin mengalami hipotermia ekstrem pada suhu beku."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.FIRE to "Panas mencairkan es dengan cepat.",
            PokemonType.FIGHTING to "Hantaman bela diri terfokus memecahkan balok es yang getas.",
            PokemonType.ROCK to "Hantaman batu padat menghancurkan struktur kristal es.",
            PokemonType.STEEL to "Logam pemecah es (icebreaker) memiliki densitas jauh lebih keras daripada es."
        ),
        defensiveResistLogic = listOf(
            "Secara defensif hanya menahan tipe Ice itu sendiri — menjadikan Ice murni tipe agresif, bukan penahan serangan."
        )
    ),
    PokemonType.FIGHTING to TypeLogicGuide(
        type = PokemonType.FIGHTING,
        archetypeTitle = "Disciplined Martial Arts & Kinetic Impact",
        coreConcept = "Serangan fisik terfokus yang menghancurkan pertahanan keras dan menumbangkan kelicikan.",
        offensiveLogic = listOf(
            PokemonType.NORMAL to "Petarung terlatih dengan mudah mengalahkan kekuatan fisik tanpa teknik.",
            PokemonType.ICE to "Pukulan karate memecahkan balok es.",
            PokemonType.ROCK to "Teknik pukulan terpusat meretakkan batu besar.",
            PokemonType.DARK to "Keadilan dan disiplin ksatria menumpas trik kotor petarung jalanan (Dark).",
            PokemonType.STEEL to "Hantaman kinetik berenergi tinggi membengkokkan pelat baja."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.FLYING to "Lawan yang terbang di udara sulit dijangkau oleh petarung jarak dekat.",
            PokemonType.PSYCHIC to "Kekuatan pikiran dan telekinesis mengungguli otot semata (Brain over Brawn).",
            PokemonType.FAIRY to "Sihir suci dan pesona magis melumpuhkan agresivitas fisik kasar."
        ),
        defensiveResistLogic = listOf(
            "Menahan Bug, Rock, dan Dark berkat ketahanan fisik serta fokus mental petarung."
        )
    ),
    PokemonType.POISON to TypeLogicGuide(
        type = PokemonType.POISON,
        archetypeTitle = "Biochemical Toxin & Corrosion",
        coreConcept = "Zat beracun yang merusak jaringan biologis tanaman dan mencemari kemurnian alam.",
        offensiveLogic = listOf(
            PokemonType.GRASS to "Racun kimia dan polusi mematikan jaringan organik tumbuhan.",
            PokemonType.FAIRY to "Polusi dan toksin merusak kemurnian alam tempat makhluk Fairy bergantung."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.GROUND to "Tanah liat dan mineral bumi menyerap serta menetralkan tumpahan racun.",
            PokemonType.PSYCHIC to "Fokus meditasi dan kekuatan mental tingkat tinggi mampu mengendalikan metabolisme terhadap racun."
        ),
        defensiveResistLogic = listOf(
            "Menahan Grass, Fighting, Poison, Bug, dan Fairy; namun 0x tidak mempan terhadap Steel karena baja adalah benda mati tanpa aliran darah."
        )
    ),
    PokemonType.GROUND to TypeLogicGuide(
        type = PokemonType.GROUND,
        archetypeTitle = "Tectonic Force & Subterranean Foundation",
        coreConcept = "Kekuatan gempa tektonik yang meruntuhkan struktur di permukaan bumi dan menyerap listrik.",
        offensiveLogic = listOf(
            PokemonType.FIRE to "Tanah dan pasir memadamkan kobaran api.",
            PokemonType.ELECTRIC to "Bumi menetralkan muatan listrik hingga tuntas.",
            PokemonType.POISON to "Mineral tanah menyerap dan mengubur limbah beracun.",
            PokemonType.ROCK to "Pergeseran lempeng tektonik menghancurkan formasi batuan.",
            PokemonType.STEEL to "Gempa bumi meruntuhkan fondasi bangunan dan konstruksi baja."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.WATER to "Air menghanyutkan dan melunakkan tanah.",
            PokemonType.GRASS to "Akar tumbuhan mengikat dan menyerap nutrisi tanah.",
            PokemonType.ICE to "Es membekukan dan meretakkan permukaan tanah."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Electric (grounding alami) serta menahan Poison dan Rock."
        )
    ),
    PokemonType.FLYING to TypeLogicGuide(
        type = PokemonType.FLYING,
        archetypeTitle = "Aerial Supremacy & High-Altitude Wind",
        coreConcept = "Keunggulan posisi di angkasa yang bebas dari guncangan darat dan menyerang dari atas.",
        offensiveLogic = listOf(
            PokemonType.GRASS to "Burung memakan tanaman dan angin kencang merontokkan dedaunan.",
            PokemonType.FIGHTING to "Serangan menukik dari udara mustahil ditangkis petarung jarak dekat di darat.",
            PokemonType.BUG to "Burung adalah predator alami bagi serangga."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.ELECTRIC to "Target terbuka bagi sambaran petir di langit.",
            PokemonType.ICE to "Suhu dingin membekukan sayap dan menjatuhkan penerbang.",
            PokemonType.ROCK to "Lemparan batu menjatuhkan burung yang sedang terbang ('killing two birds with one stone')."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Ground karena berada di udara di luar jangkauan gempa bumi; menahan Grass, Fighting, dan Bug."
        )
    ),
    PokemonType.PSYCHIC to TypeLogicGuide(
        type = PokemonType.PSYCHIC,
        archetypeTitle = "Cognitive Intellect & Telekinetic Mind",
        coreConcept = "Kekuatan pikiran manusia dan telekinesis yang mengalahkan otot serta racun, namun rentan terhadap fobia dasar manusia.",
        offensiveLogic = listOf(
            PokemonType.FIGHTING to "Akal budi dan telekinesis mengendalikan pergerakan otot petarung (Mind over Matter).",
            PokemonType.POISON to "Kekuatan pikiran dan ilmu pengetahuan menemukan penawar atas racun biologis."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.BUG to "Fobia manusia terhadap serangga merusak konsentrasi pikiran.",
            PokemonType.GHOST to "Ketakutan terhadap hantu/supranatural mengguncang kewarasan mental.",
            PokemonType.DARK to "Ketakutan akan kegelapan dan tipu daya licik mematahkan fokus psikis."
        ),
        defensiveResistLogic = listOf(
            "Menahan Fighting dan Psychic; namun serangan Psychic 0x Immune (tidak mempan) terhadap tipe Dark."
        )
    ),
    PokemonType.BUG to TypeLogicGuide(
        type = PokemonType.BUG,
        archetypeTitle = "Swarm Instinct & Chitinous Predator",
        coreConcept = "Serangga yang memakan tumbuhan dan mengganggu konsentrasi mental maupun tipu muslihat.",
        offensiveLogic = listOf(
            PokemonType.GRASS to "Serangga memakan daun, batang, dan getah tanaman.",
            PokemonType.PSYCHIC to "Serangga memicu fobia yang membuyarkan konsentrasi telekinesis.",
            PokemonType.DARK to "Insting koloni serangga tidak mempan ditipu oleh trik psikologis Dark."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.FIRE to "Api membakar tubuh kecil serangga dengan mudah.",
            PokemonType.FLYING to "Burung memangsa serangga dari udara.",
            PokemonType.ROCK to "Batu dengan mudah menggilas cangkang kitin serangga."
        ),
        defensiveResistLogic = listOf(
            "Menahan Grass, Fighting (tubuh kecil & fleksibel sulit dipukul telak), dan Ground (kaki serangga berpijak ringan di atas gempa)."
        )
    ),
    PokemonType.ROCK to TypeLogicGuide(
        type = PokemonType.ROCK,
        archetypeTitle = "Dense Mineral & Crushing Boulder",
        coreConcept = "Batuan padat yang tahan api dan menghantam target rapuh seperti es, burung, dan serangga.",
        offensiveLogic = listOf(
            PokemonType.FIRE to "Batu menimbun dan mematikan nyala api.",
            PokemonType.ICE to "Hantaman batu keras memecahkan es yang rapuh.",
            PokemonType.FLYING to "Proyektil batu menjatuhkan makhluk terbang dari udara.",
            PokemonType.BUG to "Batu besar menggilas eksoskeleton serangga."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.WATER to "Air mengikis batuan melalui erosi.",
            PokemonType.GRASS to "Akar tanaman menembus dan meretakkan celah batu.",
            PokemonType.FIGHTING to "Pukulan bela diri terpusat menghancurkan batu.",
            PokemonType.GROUND to "Gempa bumi meruntuhkan tebing batu.",
            PokemonType.STEEL to "Pahat dan bor baja memotong batuan dengan presisi."
        ),
        defensiveResistLogic = listOf(
            "Menahan Normal, Fire, Poison, dan Flying berkat kekerasan mineralnya."
        )
    ),
    PokemonType.GHOST to TypeLogicGuide(
        type = PokemonType.GHOST,
        archetypeTitle = "Spectral Apparition & Astral Terror",
        coreConcept = "Entitas tak kasatmata yang menembus fisik biasa dan meneror pikiran.",
        offensiveLogic = listOf(
            PokemonType.PSYCHIC to "Teror supranatural menghancurkan ketenangan mental pengguna Psychic.",
            PokemonType.GHOST to "Sesama entitas astral berada pada dimensi yang sama sehingga dapat saling melukai secara fatal."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.GHOST to "Serangan astral mampu menyentuh wujud hantu secara langsung.",
            PokemonType.DARK to "Energi kegelapan dan niat jahat menelan entitas arwah."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Normal & Fighting (tubuh tak berwujud tidak bisa disentuh pukulan biasa), serta menahan Poison dan Bug."
        )
    ),
    PokemonType.DRAGON to TypeLogicGuide(
        type = PokemonType.DRAGON,
        archetypeTitle = "Apex Mythical Elemental Force",
        coreConcept = "Predator puncak mitologi yang menguasai keempat elemen dasar alam (Api, Air, Listrik, Rumput).",
        offensiveLogic = listOf(
            PokemonType.DRAGON to "Kekuatan skala mistis naga hanya dapat ditandingi secara langsung oleh naga lainnya."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.ICE to "Sebagai reptil raksasa, naga sangat rentan terhadap pembekuan suhu ekstrem.",
            PokemonType.DRAGON to "Hantaman energi drakonik menembus sisik naga.",
            PokemonType.FAIRY to "Dalam dongeng kuno, sihir suci bangsa Fairy ditakdirkan menaklukkan naga."
        ),
        defensiveResistLogic = listOf(
            "Menahan keempat elemen dasar alam sekaligus: Fire, Water, Grass, dan Electric."
        )
    ),
    PokemonType.DARK to TypeLogicGuide(
        type = PokemonType.DARK,
        archetypeTitle = "Underhanded Tactics & Pragmatic Ambush",
        coreConcept = "Gaya bertarung oportunis (Aku / Evil) yang mengeksploitasi celah mental dan supranatural.",
        offensiveLogic = listOf(
            PokemonType.PSYCHIC to "Serangan mendadak dan tipu muslihat meruntuhkan fokus intelektual.",
            PokemonType.GHOST to "Energi kegelapan menguasai dan menekan roh astral."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.FIGHTING to "Disiplin bela diri murni dan keadilan mengalahkan trik curang.",
            PokemonType.BUG to "Insting serangga tidak terpengaruh intimidasi maupun tipu daya.",
            PokemonType.FAIRY to "Cahaya sihir Fairy mengusir kegelapan."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Psychic (pikiran yang tertutup kelicikan tidak dapat ditembus telepati), serta menahan Ghost dan Dark."
        )
    ),
    PokemonType.STEEL to TypeLogicGuide(
        type = PokemonType.STEEL,
        archetypeTitle = "Tempered Alloy & Ultimate Defensive Bastion",
        coreConcept = "Tipe pertahanan terbaik di seluruh generasi Pokémon dengan jumlah resistensi terbanyak.",
        offensiveLogic = listOf(
            PokemonType.ICE to "Logam baja dengan mudah memecahkan lapisan es.",
            PokemonType.ROCK to "Alat berat dan bor baja menghancurkan batuan mineral.",
            PokemonType.FAIRY to "Dalam cerita rakyat Eropa, besi tempa (Cold Iron) adalah kelemahan mutlak makhluk peri (Fairy)."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.FIRE to "Suhu tungku api melelehkan struktur baja.",
            PokemonType.FIGHTING to "Hantaman kinetik berenergi tinggi membuat perisai baja penyok.",
            PokemonType.GROUND to "Gempa tektonik meruntuhkan struktur logam dari bawah."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Poison, serta menahan 10 tipe sekaligus (11 tipe di Gen 4 HGSS termasuk Ghost & Dark karena perisai baja kuno menangkal kutukan)."
        )
    ),
    PokemonType.FAIRY to TypeLogicGuide(
        type = PokemonType.FAIRY,
        archetypeTitle = "Pure Folklore Magic & Fae Light",
        coreConcept = "Energi magis murni (diperkenalkan di Gen 6+) yang menyeimbangkan dominasi Dragon, Dark, dan Fighting.",
        offensiveLogic = listOf(
            PokemonType.FIGHTING to "Pesona dan sihir membuat kekuatan kasar petarung meleset.",
            PokemonType.DRAGON to "Sihir dongeng menaklukkan amukan naga raksasa.",
            PokemonType.DARK to "Cahaya murni menghapus energi kegelapan."
        ),
        defensiveWeakLogic = listOf(
            PokemonType.POISON to "Polusi dan racun mencemari habitat magis Fairy.",
            PokemonType.STEEL to "Besi dingin (Cold Iron) membakar dan menetralkan sihir peri."
        ),
        defensiveResistLogic = listOf(
            "0x Immune terhadap Dragon, serta menahan Fighting, Bug, dan Dark."
        )
    )
)

@Composable
fun SingleTypeLogicAnalogiesView(isModern: Boolean) {
    val activeTypes = remember(isModern) { PokemonType.activeTypes(isModern) }
    var selectedType by remember { mutableStateOf(PokemonType.WATER) }

    LaunchedEffect(isModern) {
        if (!isModern && selectedType == PokemonType.FAIRY) {
            selectedType = PokemonType.STEEL
        }
    }

    val guide = typeLogicMap[selectedType] ?: typeLogicMap[PokemonType.WATER]!!

    val deals2xTo = remember(selectedType, isModern) {
        activeTypes.filter { PokemonType.getMultiplier(selectedType, it, isModern) == 2.0f }
    }
    val resists05x = remember(selectedType, isModern) {
        activeTypes.filter { PokemonType.getMultiplier(it, selectedType, isModern) == 0.5f }
    }
    val immuneTo0x = remember(selectedType, isModern) {
        activeTypes.filter { PokemonType.getMultiplier(it, selectedType, isModern) == 0.0f }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 6x3 Elemental Type Selector Grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SELECT ELEMENTAL TYPE TO INSPECT LOGIC (${activeTypes.size} TYPES)",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val rows = BattleDrillsEngine.GRID_TYPES_ORDER.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { rowTypes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowTypes.forEach { t ->
                                val isDisabled = !isModern && t == PokemonType.FAIRY
                                val isSel = selectedType == t
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isDisabled -> Color(0xFF0F172A).copy(alpha = 0.4f)
                                                isSel -> t.color
                                                else -> Color(0xFF1F2937)
                                            }
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSel) Color.White else t.color.copy(alpha = if (isDisabled) 0.15f else 0.4f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = !isDisabled) { selectedType = t }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = t.displayName.uppercase(),
                                        color = if (isDisabled) Color(0xFF475569) else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Type Identity Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, selectedType.color, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypePill(selectedType)
                        Text(
                            text = guide.archetypeTitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = if (isModern) "Modern Chart" else "Gen 4 Chart",
                        color = NeonAmber,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = guide.coreConcept,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Section 1: Offensive Strength Logic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFENSIVE STRENGTH LOGIC (2× SUPER EFFECTIVE)",
                        color = NeonAmber,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${deals2xTo.size} Targets",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val activeOffensiveLogic = guide.offensiveLogic.filter { activeTypes.contains(it.first) }
                if (activeOffensiveLogic.isEmpty()) {
                    Text(
                        text = "Tipe ${selectedType.displayName} tidak memiliki keunggulan 2× Super Effective terhadap tipe apa pun, melainkan mengandalkan daya rusak netral konsisten.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                } else {
                    activeOffensiveLogic.forEach { (targetType, reason) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0B0F19))
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(modifier = Modifier.width(68.dp)) {
                                TypePill(targetType)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Defensive Weakness & Resistance Logic
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DEFENSIVE WEAKNESS LOGIC (INCOMING 2× THREATS)",
                    color = Color(0xFFEF4444),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                val activeWeakLogic = guide.defensiveWeakLogic.filter { activeTypes.contains(it.first) }
                activeWeakLogic.forEach { (threatType, reason) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, Color(0xFF7F1D1D).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.width(68.dp)) {
                            TypePill(threatType)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "RESISTANCE & IMMUNITY SUMMARY (${resists05x.size} RESISTS / ${immuneTo0x.size} IMMUNE)",
                    color = NeonEmerald,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                if (resists05x.isNotEmpty() || immuneTo0x.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        immuneTo0x.forEach { t -> TypePill(t) }
                        resists05x.forEach { t -> TypePill(t) }
                    }
                }

                guide.defensiveResistLogic.forEach { note ->
                    Text(
                        text = note,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
