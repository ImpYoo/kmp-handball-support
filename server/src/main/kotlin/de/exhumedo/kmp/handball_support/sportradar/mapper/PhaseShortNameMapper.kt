package de.exhumedo.kmp.handball_support.sportradar.mapper

// ── Phase name → abbreviation mapper ─────────────────────────────────────────
// Produces a short displayable code from the cleaned phase name.
// Extend KNOWN_LEAGUES for new competitions; the fallback handles unknowns.
// ─────────────────────────────────────────────────────────────────────────────

private val KNOWN_LEAGUES = mapOf(
    "DAIKIN HBL" to "HBL",
    "2. Handball-Bundesliga" to "HBL2",
    "DHB-Pokal Frauen" to "DHBPF",
    "DHB-Pokal" to "DHBPM",
    "Handball Super Cup" to "HSC",
    "Pixum Super Cup" to "PSC",
    "Bundesliga Frauen" to "HBF",
    "2. Bundesliga Frauen" to "HBF2",
)

fun String.toPhaseShortName(): String {
    // 1. Exact prefix match against known leagues
    KNOWN_LEAGUES.forEach { (prefix, code) ->
        if (startsWith(prefix)) return code
    }

    val parts = split(" ")

    // 2. "3. Liga …" → first letters of first 3 words + uppercase from qualifier word
    //    Mirrors JS logic: word[5] for 6-word names, word[6] for 7-word names.
    if (contains("3. Liga")) {
        val prefix = parts.take(3).map { it.first() }.joinToString("")
        val qualifierWord = when (parts.size) {
            6 -> parts[5]
            7 -> parts[6]
            else -> ""
        }
        val qualifier = qualifierWord.filter(Char::isUpperCase)
        return prefix + qualifier
    }

    // 3. "JBLH …" → JBLH + first two chars of next word (gender/age hint)
    if (contains("JBLH") && parts.size >= 2) {
        val hint = parts.getOrNull(1)?.take(2).orEmpty()
        return "JBLH$hint"
    }

    // 4. Unknown — return empty so the caller can log/notice
    return ""
}


