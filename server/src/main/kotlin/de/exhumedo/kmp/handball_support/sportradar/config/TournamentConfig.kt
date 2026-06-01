package de.exhumedo.kmp.handball_support.sportradar.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Tournament configuration ──────────────────────────────────────────────────
// Minimal config: only tournamentId + seasonId are required to build the URL.
// Phases and matches are loaded in full; callers filter by date as needed.
//
// KMP note: The JSON string must be supplied by the platform (JVM: classpath
// resource, Android: assets, iOS/JS: bundled file read via expect/actual).
// Use TournamentConfigLoader.fromJson(string) after loading on each platform.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TournamentConfig(
    val tournamentId: Int,
    val seasonId: Int,
    /** Selects HBL base URL (hbl.fmp.sportradar.com) vs DHB (dhbdata.fmp.sportradar.com) */
    val isHbl: Boolean = false,
    /** Human-readable label — not used at runtime, purely for config readability */
    val label: String = "",
)

object TournamentConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(content: String): List<TournamentConfig> {
        val configs = json.decodeFromString<List<TournamentConfig>>(content)
        configs.forEachIndexed { index, cfg ->
            require(cfg.tournamentId > 0) { "tournamentId must be > 0 at index $index" }
            require(cfg.seasonId > 0) { "seasonId must be > 0 at index $index" }
        }
        val duplicates = configs
            .groupingBy { it.tournamentId to it.seasonId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicates.isEmpty()) {
            "tournaments.json contains duplicate tournament/season entries: ${duplicates.joinToString()}"
        }
        return configs
    }
}

