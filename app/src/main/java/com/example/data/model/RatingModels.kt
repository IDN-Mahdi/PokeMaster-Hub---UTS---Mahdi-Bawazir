package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class RatingStatus {
    UNRATED,
    LIKE,
    DISLIKE
}

enum class RatingTag(val label: String, val shortCode: String) {
    ICON("Icon", "icon"),
    DESIGN("Design", "design"),
    PARTY("Party", "party")
}

enum class Tier(
    val label: String,
    val textColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val minBst: Int,
    val maxBst: Int
) {
    S(
        label = "S",
        textColor = Color(0xFFF87171),
        backgroundColor = Color(0x33EF4444),
        borderColor = Color(0x80EF4444),
        minBst = 580,
        maxBst = 9999
    ),
    A(
        label = "A",
        textColor = Color(0xFFFB923C),
        backgroundColor = Color(0x33F97316),
        borderColor = Color(0x80F97316),
        minBst = 520,
        maxBst = 579
    ),
    B(
        label = "B",
        textColor = Color(0xFFFACC15),
        backgroundColor = Color(0x33EAB308),
        borderColor = Color(0x80EAB308),
        minBst = 470,
        maxBst = 519
    ),
    C(
        label = "C",
        textColor = Color(0xFF4ADE80),
        backgroundColor = Color(0x3322C55E),
        borderColor = Color(0x8022C55E),
        minBst = 400,
        maxBst = 469
    ),
    D(
        label = "D",
        textColor = Color(0xFF60A5FA),
        backgroundColor = Color(0x333B82F6),
        borderColor = Color(0x803B82F6),
        minBst = 300,
        maxBst = 399
    ),
    F(
        label = "F",
        textColor = Color(0xFFCBD5E1),
        backgroundColor = Color(0x3394A3B8),
        borderColor = Color(0x8094A3B8),
        minBst = 0,
        maxBst = 299
    ),
    TRAP(
        label = "TRAP",
        textColor = Color(0xFFFDA4AF),
        backgroundColor = Color(0x33F43F5E),
        borderColor = Color(0xFFF43F5E),
        minBst = 0,
        maxBst = 9999
    );

    companion object {
        fun fromBst(bst: Int, isTrap: Boolean = false): Tier {
            if (isTrap) return TRAP
            return entries.firstOrNull { it != TRAP && bst >= it.minBst && bst <= it.maxBst } ?: F
        }

        fun fromString(str: String?): Tier? {
            if (str == null) return null
            return entries.firstOrNull { it.name.equals(str, ignoreCase = true) }
        }
    }
}

enum class TrackerStatus(val displayName: String, val badgeColor: Color) {
    UNSEEN("Unseen", Color(0xFF64748B)),
    SEEN("Seen", Color(0xFF38BDF8)),
    CAUGHT("Caught", Color(0xFF10B981))
}
