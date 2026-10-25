package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.api.PokemonLiveDetail
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun PokemonDetailModal(
    pokemon: Pokemon,
    rating: PokemonRating?,
    familyMembers: List<Pokemon>,
    isModern: Boolean,
    isShiny: Boolean,
    isPlayingCry: Boolean,
    liveDetail: PokemonLiveDetail?,
    isLiveSyncing: Boolean,
    isPreferredShiny: Boolean = false,
    onTogglePreferredShiny: ((Boolean) -> Unit)? = null,
    isPartyMember: Boolean = false,
    onTogglePartyMember: (() -> Unit)? = null,
    onPlayCry: () -> Unit,
    onRefreshLiveApi: () -> Unit,
    onSelectFamilyStage: (Pokemon) -> Unit,
    onSetTier: (Tier?) -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        var isDetailShiny by remember(isShiny, isPreferredShiny) { mutableStateOf(isPreferredShiny || isShiny) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                .testTag("pokemon_detail_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${pokemon.name} ${pokemon.formattedId()}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            TypePill(pokemon.primaryType)
                            pokemon.secondaryType?.let { TypePill(it) }
                            if (pokemon.johtoDexNo != null) {
                                Text(
                                    text = "Johto #${pokemon.johtoDexNo}",
                                    color = NeonAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("btn_close_detail")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live REST API Status Indicator Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF064E3B).copy(alpha = 0.4f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live API Connected: pokeapi.co/api/v2",
                            color = Color(0xFF6EE7B7),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onRefreshLiveApi,
                        modifier = Modifier.size(24.dp)
                    ) {
                        if (isLiveSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF6EE7B7), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Live API", tint = Color(0xFF6EE7B7), modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Artwork & Audio Cry Banner with ✦ Shiny Toggle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFF0B1120), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = if (isDetailShiny) pokemon.spriteUrl(true) else pokemon.officialArtworkUrl(),
                        contentDescription = pokemon.name,
                        modifier = Modifier.size(145.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Appearance: [Normal] | [✦ Shiny] Toggle (Per-Pokémon Preferred Appearance)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Appearance:",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (!isDetailShiny) Color(0xFF334155) else Color.Transparent)
                                .clickable {
                                    isDetailShiny = false
                                    onTogglePreferredShiny?.invoke(false)
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("btn_detail_appearance_normal"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Normal",
                                color = if (!isDetailShiny) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isDetailShiny) NeonAmber.copy(alpha = 0.35f) else Color.Transparent)
                                .border(
                                    if (isDetailShiny) 1.dp else 0.dp,
                                    if (isDetailShiny) NeonAmber else Color.Transparent,
                                    RoundedCornerShape(5.dp)
                                )
                                .clickable {
                                    isDetailShiny = true
                                    onTogglePreferredShiny?.invoke(true)
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("btn_detail_appearance_shiny"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "✦",
                                    color = if (isDetailShiny) NeonAmber else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Shiny",
                                    color = if (isDetailShiny) NeonAmber else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Audio Cry Play Button
                    Button(
                        onClick = onPlayCry,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .height(32.dp)
                            .testTag("btn_play_cry"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlayingCry) NeonEmerald else NeonBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlayingCry) "Playing Cry..." else "Play Cry",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // + Add to Party Lab Action Button
                if (onTogglePartyMember != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onTogglePartyMember,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("btn_detail_toggle_party"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPartyMember) Color(0xFF10B981) else Color(0xFFE11D48),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isPartyMember) "✓ Assigned to Party Lab (Tap to Remove)" else "+ Add to Party Lab",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                // Live Pokédex Flavor Text (from PokeAPI Species endpoint)
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161C28))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "OFFICIAL POKÉDEX FLAVOR TEXT (POKÉAPI v2)",
                            color = NeonAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = liveDetail?.flavorText ?: "Synchronizing official English Pokédex description from live API...",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        if (liveDetail != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Height: ${liveDetail.heightMeters} m", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text("Weight: ${liveDetail.weightKg} kg", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Text("Base Exp: ${liveDetail.baseExperience}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                if (liveDetail.habitat != null) Text("Habitat: ${liveDetail.habitat}", color = Color(0xFF94A3B8), fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Noob Trap Alert
                if (pokemon.isTrap) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Tier.TRAP.borderColor, RoundedCornerShape(10.dp))
                            .background(Tier.TRAP.backgroundColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Tier.TRAP.textColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "COMPETITIVE NOOB TRAP DETECTED",
                                    color = Tier.TRAP.textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = pokemon.trapReason ?: "Suboptimal stats or severely exploitable ability in competitive play.",
                                    color = Color(0xFFFEE2E2),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Family Evolution Line (Stage Selector Pills)
                if (familyMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "EVOLUTION FAMILY LINE (${familyMembers.size} STAGES)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        familyMembers.forEach { member ->
                            val isCurrent = member.id == pokemon.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) NeonBlue.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isCurrent) NeonBlue else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { onSelectFamilyStage(member) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = member.name,
                                        color = if (isCurrent) NeonBlue else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "BST ${member.bst}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Base Stats Overview
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BASE STAT SPREAD",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .background(NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .border(1.dp, NeonBlue, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BST ${pokemon.bst}",
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("HP", pokemon.hp, Color(0xFFEF4444))
                StatRow("Attack", pokemon.attack, Color(0xFFF97316))
                StatRow("Defense", pokemon.defense, Color(0xFFEAB308))
                StatRow("Sp. Atk", pokemon.spAtk, Color(0xFF3B82F6))
                StatRow("Sp. Def", pokemon.spDef, Color(0xFF10B981))
                StatRow("Speed", pokemon.speed, Color(0xFFA855F7))

                // Type Effectiveness Matrix (Gen 4 vs Modern sensitive)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "DEFENSIVE TYPE EFFECTIVENESS (${if (isModern) "MODERN GEN 9" else "GEN 4 HGSS"})",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                TypeEffectivenessSection(pokemon, isModern)

                // Custom Tier Override Selector
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CUSTOM TIER OVERRIDE",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Tier.entries.forEach { t ->
                        val isSelected = (rating?.customTier ?: pokemon.defaultTier) == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) t.backgroundColor else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) t.borderColor else Color(0xFF334155), RoundedCornerShape(6.dp))
                                .clickable { onSetTier(t) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.label,
                                color = if (isSelected) t.textColor else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: Int, barColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            modifier = Modifier.width(65.dp)
        )
        Text(
            text = "$value",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(35.dp)
        )
        LinearProgressIndicator(
            progress = { (value / 255f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun TypeEffectivenessSection(pokemon: Pokemon, isModern: Boolean) {
    val allActiveTypes = PokemonType.activeTypes(isModern)
    val weak4x = mutableListOf<PokemonType>()
    val weak2x = mutableListOf<PokemonType>()
    val resist05x = mutableListOf<PokemonType>()
    val resist025x = mutableListOf<PokemonType>()
    val immune0x = mutableListOf<PokemonType>()

    allActiveTypes.forEach { attackType ->
        val mult = PokemonType.getDefendingMultiplier(attackType, pokemon.primaryType, pokemon.secondaryType, isModern)
        when (mult) {
            4.0f -> weak4x.add(attackType)
            2.0f -> weak2x.add(attackType)
            0.5f -> resist05x.add(attackType)
            0.25f -> resist025x.add(attackType)
            0.0f -> immune0x.add(attackType)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (weak4x.isNotEmpty()) MultiplierRow("4x Weak", weak4x, Color(0xFFEF4444))
        if (weak2x.isNotEmpty()) MultiplierRow("2x Weak", weak2x, Color(0xFFF97316))
        if (resist05x.isNotEmpty()) MultiplierRow("0.5x Resist", resist05x, Color(0xFF10B981))
        if (resist025x.isNotEmpty()) MultiplierRow("0.25x Resist", resist025x, Color(0xFF059669))
        if (immune0x.isNotEmpty()) MultiplierRow("0x Immune", immune0x, Color(0xFF64748B))
    }
}

@Composable
fun MultiplierRow(label: String, types: List<PokemonType>, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(75.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            types.forEach { t ->
                TypePill(t)
            }
        }
    }
}
