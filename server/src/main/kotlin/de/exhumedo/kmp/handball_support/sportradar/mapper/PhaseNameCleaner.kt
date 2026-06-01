package de.exhumedo.kmp.handball_support.sportradar.mapper

// ── Season-suffix cleaner ─────────────────────────────────────────────────────
// Regex approach: season-agnostic, never needs updating for new seasons.
//
// Matches patterns like:
//   " (2025/26)"   " (2025/2026)"   " 25/26"   " 2025/2026"   "2025/26"
//
// Pattern breakdown:
//   \s*       — optional leading whitespace
//   \(?       — optional opening paren
//   \d{2,4}   — 2- or 4-digit year  (e.g. 25 or 2025)
//   /         — literal slash
//   \d{2,4}   — 2- or 4-digit year  (e.g. 26 or 2026)
//   \)?       — optional closing paren
// ─────────────────────────────────────────────────────────────────────────────

private val SEASON_SUFFIX_REGEX = Regex("""\s*\(?\d{2,4}/\d{2,4}\)?""")

fun String.cleanSeasonSuffix(): String =
    replace(SEASON_SUFFIX_REGEX, "").trim()

