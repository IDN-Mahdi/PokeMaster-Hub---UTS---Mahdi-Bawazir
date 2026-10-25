package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.ProfileBadgeSettings
import com.example.ui.theme.*

private val CardUnratedBg = Color(0xFF131D2E)
private val CardUnratedBorder = Color(0xFF24324D)
private val CardDislikeBg = Color(0xFF1E1B4B) // Deep Indigo Slate
private val CardDislikeBorder = Color(0xFF3730A3)

// Luxury Liked Hue Palette (Strictly ordered Top-to-Bottom: 1. Party Red -> 2. Icon Gold -> 3. Design Platinum)
private val PartyRedColor = Color(0xFFE11D48) // Crimson Ruby Red
private val IconGoldColor = Color(0xFFF59E0B) // Royal Amber Gold
private val DesignPlatinumColor = Color(0xFFF1F5F9) // Alabaster Platinum / Pearl White

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PokemonCard(
    pokemon: Pokemon,
    rating: PokemonRating?,
    isPartyMember: Boolean,
    isShiny: Boolean,
    onToggleTag: (RatingTag) -> Unit,
    onDislike: () -> Unit,
    onUndo: () -> Unit,
    onOpenDetail: () -> Unit,
    onSetTier: (Tier?) -> Unit,
    modifier: Modifier = Modifier,
    displayDexNumber: String = pokemon.formattedId(),
    badgeSettings: ProfileBadgeSettings = ProfileBadgeSettings(),
    isFamilyMode: Boolean = false,
    familyStages: List<Pokemon> = emptyList(),
    onSelectStage: ((Pokemon) -> Unit)? = null,
    typeEra: TypeEra = TypeEra.ALL_TYPES,
    gridColumns: Int = 2
) {
    var showTierMenu by remember { mutableStateOf(false) }

    val status = rating?.status ?: RatingStatus.UNRATED
    val tags = rating?.tags ?: emptySet()
    val activeTier = rating?.customTier ?: pokemon.defaultTier

    // Calculate effective era types (auto-syncs 22 pre-Fairy Pokémon & Gen 1 Magnemite/Magneton)
    val (effectivePrimary, effectiveSecondary) = remember(pokemon, typeEra) {
        pokemon.effectiveTypes(typeEra)
    }

    // Split Collage Colors strictly ordered Top-to-Bottom: 1. Party (Red) -> 2. Icon (Gold) -> 3. Design (Platinum)
    val sortedLikedColors = remember(tags) {
        val list = mutableListOf<Color>()
        if (tags.contains(RatingTag.PARTY)) list.add(PartyRedColor)
        if (tags.contains(RatingTag.ICON)) list.add(IconGoldColor)
        if (tags.contains(RatingTag.DESIGN)) list.add(DesignPlatinumColor)
        list
    }

    val topBgColor = remember(status, sortedLikedColors) {
        when {
            status == RatingStatus.LIKE && sortedLikedColors.isNotEmpty() -> sortedLikedColors.first()
            status == RatingStatus.DISLIKE -> CardDislikeBg
            else -> CardUnratedBg
        }
    }

    val bottomBgColor = remember(status, sortedLikedColors) {
        when {
            status == RatingStatus.LIKE && sortedLikedColors.isNotEmpty() -> sortedLikedColors.last()
            status == RatingStatus.DISLIKE -> CardDislikeBg
            else -> CardUnratedBg
        }
    }

    // Adaptive Contrast Ink: Deep Obsidian #0F172A on Gold/Platinum, Crisp White #FFFFFF on Dark/Red/Indigo
    val isTopLight = topBgColor == DesignPlatinumColor || topBgColor == IconGoldColor
    val isBottomLight = bottomBgColor == DesignPlatinumColor || bottomBgColor == IconGoldColor

    val topDexColor = if (isTopLight) Color(0xFF0F172A) else Color.White
    val topBstColor = when {
        isTopLight -> Color(0xFF1E293B)
        topBgColor == PartyRedColor -> Color(0xFFFFE4E6)
        else -> Color(0xFF93C5FD)
    }
    val bottomNameColor = if (isBottomLight) Color(0xFF0F172A) else Color.White

    val solidTierBg = when (activeTier) {
        Tier.S -> Color(0xFF7C3AED)
        Tier.A -> Color(0xFFEA580C)
        Tier.B -> Color(0xFFD97706)
        Tier.C -> Color(0xFF059669)
        Tier.D -> Color(0xFF2563EB)
        Tier.F -> Color(0xFF475569)
        Tier.TRAP -> Color(0xFFDC2626)
    }

    val tierSingleLetter = if (activeTier == Tier.TRAP) "F" else activeTier.label

    val cardBorderColor = remember(status, tags) {
        when {
            status == RatingStatus.LIKE && tags.isNotEmpty() -> {
                when {
                    tags.contains(RatingTag.PARTY) -> Color(0xFFF43F5E)
                    tags.contains(RatingTag.ICON) -> Color(0xFFFBBF24)
                    else -> Color(0xFFF8FAFC)
                }
            }
            status == RatingStatus.DISLIKE -> CardDislikeBorder
            else -> CardUnratedBorder
        }
    }

    // Evolution stage pills ONLY appear when Family Mode (↑) is active
    val showEvolutionPills = isFamilyMode && familyStages.size > 1 && onSelectStage != null

    // Responsive sizing based on 1..5 columns
    val isMicroGrid = gridColumns >= 4
    val isThreeCol = gridColumns == 3
    val isOneCol = gridColumns == 1

    val cardHeight: Dp = when (gridColumns) {
        1 -> if (showEvolutionPills) 292.dp else 252.dp
        2 -> if (showEvolutionPills) 304.dp else 264.dp
        3 -> if (showEvolutionPills) 232.dp else 202.dp
        4 -> if (showEvolutionPills) 178.dp else 154.dp
        else -> if (showEvolutionPills) 158.dp else 136.dp
    }

    val spriteSize: Dp = when (gridColumns) {
        1 -> 146.dp
        2 -> 132.dp
        3 -> 88.dp
        4 -> 62.dp
        else -> 48.dp
    }

    val cardPaddingH: Dp = when {
        gridColumns >= 4 -> 5.dp
        gridColumns == 3 -> 6.dp
        else -> 9.dp
    }
    val cardPaddingV: Dp = when {
        gridColumns >= 4 -> 5.dp
        gridColumns == 3 -> 6.dp
        else -> 8.dp
    }

    val shortDexStr = remember(displayDexNumber, pokemon.id, gridColumns) {
        if (gridColumns >= 3 && displayDexNumber.startsWith("#0") && displayDexNumber.length == 5) {
            // Convert #0001 -> #001 for 3, 4, 5 columns to save horizontal space
            "#" + displayDexNumber.substring(2)
        } else {
            displayDexNumber
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .border(if (isMicroGrid) 1.dp else 1.5.dp, cardBorderColor, RoundedCornerShape(if (isMicroGrid) 12.dp else 16.dp))
            .testTag("pokemon_card_${pokemon.id}"),
        shape = RoundedCornerShape(if (isMicroGrid) 12.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isVeryNarrow = maxWidth < 95.dp

            // LAYER 1: Background Color / Multi-Like Horizontal Split Collage
            when {
                status == RatingStatus.LIKE && sortedLikedColors.isNotEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        sortedLikedColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(color)
                            )
                        }
                    }
                }
                status == RatingStatus.DISLIKE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardDislikeBg)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CardUnratedBg)
                    )
                }
            }

            // LAYER 2: 4 Invisible Hit-Zones (Top: 3 Vertical Columns for Design/Icon/Party; Bottom: 1-Click Dislike, 2-Click Undo; Hold: Detail Modal)
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // 1/3 Top-Left: Toggle Like Design (Alabaster Platinum #F1F5F9)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = { onToggleTag(RatingTag.DESIGN) },
                                onLongClick = onOpenDetail
                            )
                            .testTag("hit_zone_top_left_design_${pokemon.id}")
                    )
                    // 1/3 Top-Middle: Toggle Like Icon (Royal Amber Gold #F59E0B)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = { onToggleTag(RatingTag.ICON) },
                                onLongClick = onOpenDetail
                            )
                            .testTag("hit_zone_top_mid_icon_${pokemon.id}")
                    )
                    // 1/3 Top-Right: Toggle Like Party (Crimson Ruby Red #E11D48)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = { onToggleTag(RatingTag.PARTY) },
                                onLongClick = onOpenDetail
                            )
                            .testTag("hit_zone_top_right_party_${pokemon.id}")
                    )
                }

                // Bottom Half: 1-Click = Dislike (#1E1B4B), Double-Click = Undo/Reset to Unrated, Hold = Detail Modal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onDislike() },
                            onDoubleClick = { onUndo() },
                            onLongClick = onOpenDetail
                        )
                        .testTag("hit_zone_bottom_dislike_undo_${pokemon.id}")
                )
            }

            // LAYER 3: Seamless Panel-less Content Overlay with Global Anti-Wrap Text Guard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = cardPaddingH, vertical = cardPaddingV),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP METADATA ROW
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
                            isMicroGrid || isVeryNarrow -> {
                                // 4 & 5 Columns: Only #001 on top left to guarantee zero collision
                                Text(
                                    text = shortDexStr,
                                    color = topDexColor,
                                    fontSize = if (gridColumns >= 5) 7.5.sp else 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            isThreeCol -> {
                                // 3 Columns: Compact "#001 • 318" format
                                val compactHeader = if (badgeSettings.showBst) "$shortDexStr • ${pokemon.bst}" else shortDexStr
                                Text(
                                    text = compactHeader,
                                    color = topDexColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            else -> {
                                // 1 & 2 Columns: Spacious "#0001  BST: 318"
                                Text(
                                    text = displayDexNumber,
                                    color = topDexColor,
                                    fontSize = if (isOneCol) 12.sp else 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (badgeSettings.showBst) {
                                    Text(
                                        text = "BST: ${pokemon.bst}",
                                        color = topBstColor,
                                        fontSize = if (isOneCol) 11.sp else 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (pokemon.isLegendary && badgeSettings.showLegendaryMythical) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isTopLight) Color(0xFF0F172A) else NeonAmber.copy(alpha = 0.3f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "★",
                                            color = if (isTopLight) NeonAmber else NeonAmber,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                if (pokemon.isSingular && badgeSettings.showSingularNoEvo) {
                                    Text(
                                        text = "∅",
                                        color = if (isTopLight) Color(0xFF581C87) else SingularFuchsiaText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (pokemon.isTrap && badgeSettings.showNoobTrap) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFDC2626))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "TRAP",
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Right: Single-letter solid Tier Badge (S, A, B, C, D, F)
                    if (badgeSettings.showTierRating) {
                        Box {
                            val badgeBoxSize = when {
                                gridColumns >= 5 -> 15.dp
                                gridColumns == 4 -> 16.dp
                                gridColumns == 3 -> 19.dp
                                else -> 22.dp
                            }
                            val badgeFontSize = when {
                                gridColumns >= 4 -> 7.5.sp
                                gridColumns == 3 -> 9.sp
                                else -> 10.5.sp
                            }
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = badgeBoxSize, minHeight = badgeBoxSize)
                                    .clip(RoundedCornerShape(if (isMicroGrid) 4.dp else 5.dp))
                                    .background(solidTierBg)
                                    .clickable { showTierMenu = true }
                                    .padding(horizontal = if (isMicroGrid) 3.dp else 6.dp, vertical = 1.dp)
                                    .testTag("tier_badge_${pokemon.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tierSingleLetter,
                                    color = Color.White,
                                    fontSize = badgeFontSize,
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

                // MIDDLE: Pokémon Sprite (20% larger and centered cleanly)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = pokemon.spriteUrl(isShiny),
                        contentDescription = pokemon.name,
                        modifier = Modifier.size(spriteSize),
                        contentScale = ContentScale.Fit
                    )
                }

                // BOTTOM: Pokémon Name + Responsive Type Pills (Never clipped or wrapped vertically!)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pokemon.name,
                            color = bottomNameColor,
                            fontSize = when (gridColumns) {
                                1 -> 15.sp
                                2 -> 13.5.sp
                                3 -> 11.5.sp
                                4 -> 9.5.sp
                                else -> 8.5.sp
                            },
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isMicroGrid && badgeSettings.showBst && gridColumns == 4) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${pokemon.bst}",
                                color = if (isBottomLight) Color(0xFF334155) else Color(0xFF93C5FD),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isMicroGrid) 2.dp else 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isMicroGrid || isThreeCol) 3.dp else 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypePill(
                            type = effectivePrimary,
                            useShortCode = isMicroGrid || isVeryNarrow,
                            compact = isThreeCol || isMicroGrid
                        )
                        effectiveSecondary?.let {
                            TypePill(
                                type = it,
                                useShortCode = isMicroGrid || isVeryNarrow,
                                compact = isThreeCol || isMicroGrid
                            )
                        }
                    }
                }

                // Optional Evolution Family Stage Selector Pills (ONLY visible when ↑ Family Mode is active)
                if (showEvolutionPills) {
                    Spacer(modifier = Modifier.height(if (isMicroGrid) 2.dp else 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        familyStages.take(4).forEach { stage ->
                            val isCurrentStage = stage.id == pokemon.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isCurrentStage) {
                                            if (isBottomLight) Color(0xFF0F172A) else Color(0xFF1E293B)
                                        } else {
                                            if (isBottomLight) Color(0x280F172A) else Color(0x44000000)
                                        }
                                    )
                                    .border(
                                        0.8.dp,
                                        if (isCurrentStage) NeonBlue else Color(0x22FFFFFF),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSelectStage?.invoke(stage) }
                                    .padding(vertical = 2.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stage.name,
                                    color = if (isCurrentStage) {
                                        if (isBottomLight) Color.White else NeonBlue
                                    } else {
                                        if (isBottomLight) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                    },
                                    fontSize = if (isMicroGrid) 6.5.sp else 7.5.sp,
                                    fontWeight = if (isCurrentStage) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypePill(
    type: PokemonType,
    modifier: Modifier = Modifier,
    useShortCode: Boolean = false,
    compact: Boolean = false,
    fontSizeOverride: TextUnit? = null
) {
    val label = if (useShortCode) type.shortCode else type.displayName.uppercase()
    val fontSize = fontSizeOverride ?: when {
        useShortCode -> 7.5.sp
        compact -> 7.5.sp
        else -> 8.5.sp
    }
    val hPad = when {
        useShortCode -> 4.dp
        compact -> 4.dp
        else -> 6.dp
    }
    val vPad = if (useShortCode || compact) 1.dp else 1.5.dp

    // High-contrast ink on bright type badges (Electric/Ice/Normal/Ground/Steel) vs white ink
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(type.color)
            .border(0.5.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
