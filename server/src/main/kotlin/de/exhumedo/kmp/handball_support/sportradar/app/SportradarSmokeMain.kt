package de.exhumedo.kmp.handball_support.sportradar.app

import de.exhumedo.kmp.handball_support.sportradar.client.SportradarHttpClientFactory
import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfigLoader
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarResult
import de.exhumedo.kmp.handball_support.sportradar.repository.SportradarPhaseRepository
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tiny smoke runner for real Sportradar connectivity and mapping.
 *
 * Usage (from project root):
 *   ./gradlew :server:runSportradarSmoke
 *
 * Reads tournaments from [TOURNAMENTS_FILE] (relative to working directory).
 */
private const val TOURNAMENTS_FILE = "server/data/tournaments.json"

fun main() = runBlocking {
    val tournamentsPath = Paths.get(TOURNAMENTS_FILE)
    check(Files.exists(tournamentsPath)) {
        "tournaments.json not found at '$tournamentsPath' — run from the project root"
    }

    val configs = TournamentConfigLoader.fromJson(Files.readString(tournamentsPath))
    check(configs.isNotEmpty()) { "tournaments.json is empty" }

    val repo = SportradarPhaseRepository(
        httpClient = SportradarHttpClientFactory.createJvmDefault(),
        configs    = configs.take(1),
    )

    when (val result = repo.getAllPhases()) {
        is SportradarResult.Success -> {
            println("Sportradar smoke OK: phases=${result.value.size}")
            result.value.firstOrNull()?.also {
                println("First phase: id=${it.phaseId}, name='${it.name}', matchDays=${it.matchDays.size}")
            }
        }
        is SportradarResult.Failure ->
            error("Sportradar smoke FAILED: ${result.error}")
    }
}
