package de.exhumedo.kmp.handball_support.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Central spacing / sizing scale so the whole app breathes consistently.
 * Based on a 4 dp grid — the foundation of a clean, modern layout.
 */
object Dimens {
    /** 4 dp */
    val spaceXs = 4.dp
    /** 8 dp */
    val spaceSm = 8.dp
    /** 12 dp */
    val spaceMd = 12.dp
    /** 16 dp */
    val spaceLg = 16.dp
    /** 24 dp */
    val spaceXl = 24.dp
    /** 32 dp */
    val spaceXxl = 32.dp

    /** Maximum readable content width on large screens; content centers within it. */
    val contentMaxWidth = 880.dp

    /** Card / surface corner radius. */
    val cardCorner = 16.dp
    /** Resting card shadow. */
    val cardElevation = 2.dp
    /** Raised (selected/hovered) card shadow. */
    val cardElevationRaised = 6.dp

    /** Height of the short red accent bar shown before section titles. */
    val accentBarHeight = 22.dp
    val accentBarWidth = 4.dp
}

