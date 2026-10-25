package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RecentRatingAction
import com.example.data.model.RecentTrackerAction
import com.example.data.model.TrackerStatus
import com.example.ui.PokedexPageMode
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurfaceHeader
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBlue
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryTab(val label: String) {
    PICKER("Picker History"),
    TRACKER("Tracker History")
}

@Composable
fun RecentRatingsModal(
    recentActions: List<RecentRatingAction>,
    recentTrackerActions: List<RecentTrackerAction> = emptyList(),
    currentMode: PokedexPageMode = PokedexPageMode.LOVE_EM_ALL_PICKER,
    onUndoAction: (RecentRatingAction) -> Unit,
    onUndoTrackerAction: (RecentTrackerAction) -> Unit = {},
    onClose: () -> Unit
) {
    val initialTab = if (currentMode == PokedexPageMode.CATCH_EM_ALL_TRACKER) HistoryTab.TRACKER else HistoryTab.PICKER
    var selectedTab by remember { mutableStateOf(initialTab) }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                .testTag("recent_ratings_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceHeader)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedTab == HistoryTab.PICKER) "Recent Ratings History" else "Recent Tracker Updates",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tap Undo to revert to previous status",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("btn_close_recent")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher between Picker & Tracker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    HistoryTab.entries.forEach { tab ->
                        val isSel = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSel) {
                                        if (tab == HistoryTab.PICKER) NeonAmber else Color(0xFF10B981)
                                    } else Color.Transparent
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                color = if (isSel) Color(0xFF0B0F19) else Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == HistoryTab.PICKER) {
                    if (recentActions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent ratings recorded yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentActions) { action ->
                                RecentActionRow(action, onUndo = { onUndoAction(action) })
                            }
                        }
                    }
                } else {
                    if (recentTrackerActions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent tracker updates recorded yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentTrackerActions) { action ->
                                RecentTrackerActionRow(action, onUndo = { onUndoTrackerAction(action) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentActionRow(action: RecentRatingAction, onUndo: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(action.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161C28))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = action.pokemonName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            if (action.actionType == "LIKE") Color(0x3310B981) else Color(0x33EF4444),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = action.actionType,
                        color = if (action.actionType == "LIKE") Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Was: ${action.previousStatus.name} at $formattedTime",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }

        // Quick Undo Button
        Button(
            onClick = onUndo,
            modifier = Modifier
                .height(28.dp)
                .testTag("btn_undo_recent_${action.id}"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = NeonBlue),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecentTrackerActionRow(action: RecentTrackerAction, onUndo: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(action.timestamp))

    val statusColor = when (action.newStatus) {
        TrackerStatus.CAUGHT -> Color(0xFF10B981)
        TrackerStatus.SEEN -> Color(0xFF38BDF8)
        TrackerStatus.UNSEEN -> Color(0xFF94A3B8)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161C28))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = action.pokemonName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = action.newStatus.name,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Was: ${action.previousStatus.name} • $formattedTime",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }

        Button(
            onClick = onUndo,
            modifier = Modifier
                .height(28.dp)
                .testTag("btn_undo_tracker_${action.id}"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = NeonBlue),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Undo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
