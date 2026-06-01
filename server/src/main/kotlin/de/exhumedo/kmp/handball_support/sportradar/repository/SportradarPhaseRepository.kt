package de.exhumedo.kmp.handball_support.sportradar.repository

import de.exhumedo.kmp.handball_support.sportradar.model.Phase
import de.exhumedo.kmp.handball_support.sportradar.model.PhaseRef
import de.exhumedo.kmp.handball_support.sportradar.model.SeasonRef
import de.exhumedo.kmp.handball_support.sportradar.model.TournamentRef
import de.exhumedo.kmp.handball_support.sportradar.NoOpSportradarTelemetry
import de.exhumedo.kmp.handball_support.sportradar.SportradarTelemetry
import de.exhumedo.kmp.handball_support.sportradar.cache.FixturesCache
import de.exhumedo.kmp.handball_support.sportradar.client.KtorSportradarClient
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarClientOptions
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarClient
import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfig
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarError
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarResult
import de.exhumedo.kmp.handball_support.sportradar.mapper.SportradarMapper

import io.ktor.client.HttpClient

// ── Sportradar phase repository ───────────────────────────────────────────────
// Composes client + mapper + (optional) cache into a single fetch-all call.
//
// Fixtures caching: each tournament+season pair is cached in FixturesCache
//   under its own key; pass null to disable entirely.
//
// Tournament-list caching: always cached in-process for TOURNAMENT_CACHE_TTL_MILLIS
//   (default 5 min), independent of the fixtures cache.
//
// Error strategy: any single tournament failure aborts and propagates the typed
//   SportradarError — no silent swallowing.
// ─────────────────────────────────────────────────────────────────────────────

class SportradarPhaseRepository internal constructor(
    private val client: SportradarClient,
    private val configs: List<TournamentConfig>,
    private val cache: FixturesCache? = null,
    private val telemetry: SportradarTelemetry = NoOpSportradarTelemetry,
) {
    data class RefreshSummary(
        val tournamentCount: Int,
        val configuredSeasonCount: Int,
        val phaseCount: Int,
    )

    // ── Tournament-list in-process cache ──────────────────────────────────────
    private data class TournamentListEntry(val data: List<TournamentRef>, val expiresAt: Long)

    @Volatile private var tournamentListCache: TournamentListEntry? = null

    companion object {
        private const val TOURNAMENT_CACHE_TTL_MILLIS: Long = 5 * 60 * 1_000L

        /** Public factory — callers supply an HttpClient and never touch internal types. */
        operator fun invoke(
            httpClient: HttpClient,
            configs: List<TournamentConfig>,
            cache: FixturesCache? = null,
            clientOptions: SportradarClientOptions = SportradarClientOptions(),
            telemetry: SportradarTelemetry = NoOpSportradarTelemetry,
        ): SportradarPhaseRepository = SportradarPhaseRepository(
            client = KtorSportradarClient(
                httpClient = httpClient,
                options = clientOptions,
                telemetry = telemetry,
            ),
            configs = configs,
            cache = cache,
            telemetry = telemetry,
        )
    }

    /**
     * Fetches the full tournament list from the Sportradar /tournaments feed.
     * Results are cached in-process for [TOURNAMENT_CACHE_TTL_MILLIS] (5 min).
     */
    suspend fun getTournaments(): SportradarResult<List<TournamentRef>> {
        val hit = tournamentListCache
        if (hit != null && System.currentTimeMillis() < hit.expiresAt) {
            telemetry.onCacheHit("tournaments")
            return SportradarResult.Success(hit.data)
        }
        telemetry.onCacheMiss("tournaments")

        return when (val result = client.fetchTournamentList()) {
            is SportradarResult.Success -> {
                val tournaments = result.value.doc.firstOrNull()?.data?.tournaments ?: emptyList()
                val mapped = tournaments.map { t ->
                    TournamentRef(
                        id = t.id,
                        name = t.name,
                        seasons = t.seasons.map { s ->
                            SeasonRef(
                                id = s.id,
                                name = s.name,
                                year = s.year,
                                status = s.status,
                                phases = s.phases.map { p ->
                                    PhaseRef(
                                        id = p.id,
                                        name = p.name,
                                        startDate = p.startDate?.uts?.toString() ?: "",
                                        endDate   = p.endDate?.uts?.toString() ?: "",
                                    )
                                },
                            )
                        },
                    )
                }
                tournamentListCache = TournamentListEntry(
                    data = mapped,
                    expiresAt = System.currentTimeMillis() + TOURNAMENT_CACHE_TTL_MILLIS,
                )
                telemetry.onCacheWrite("tournaments", mapped.size)
                SportradarResult.Success(mapped)
            }
            is SportradarResult.Failure -> result
        }
    }

    suspend fun getAllPhases(): SportradarResult<List<Phase>> {
        val result = mutableListOf<Phase>()
        for (config in configs) {
            when (val r = fetchPhases(config)) {
                is SportradarResult.Success -> result += r.value
                is SportradarResult.Failure -> return r   // propagate immediately
            }
        }
        return SportradarResult.Success(result)
    }

    /**
     * Clears all in-memory caches and eagerly reloads:
     *  1) tournament list feed
     *  2) fixtures for every configured tournament+season pair
     */
    suspend fun refreshCaches(): SportradarResult<RefreshSummary> {
        cache?.invalidateAll()
        tournamentListCache = null

        val tournamentCount = when (val tr = getTournaments()) {
            is SportradarResult.Success -> tr.value.size
            is SportradarResult.Failure -> return SportradarResult.Failure(tr.error)
        }

        var phaseCount = 0
        for (config in configs) {
            when (val r = fetchPhases(config)) {
                is SportradarResult.Success -> phaseCount += r.value.size
                is SportradarResult.Failure -> return SportradarResult.Failure(r.error)
            }
        }

        return SportradarResult.Success(
            RefreshSummary(
                tournamentCount = tournamentCount,
                configuredSeasonCount = configs.size,
                phaseCount = phaseCount,
            ),
        )
    }

    /**
     * Fetches phases only for the given tournament+season pair.
     * Returns [SportradarError.EmptyResponse] when the combination is not configured.
     */
    suspend fun getPhasesForSeason(tournamentId: Int, seasonId: Int): SportradarResult<List<Phase>> {
        val config = configs.find { it.tournamentId == tournamentId && it.seasonId == seasonId }
            ?: return SportradarResult.Failure(SportradarError.EmptyResponse)
        return fetchPhases(config)
    }

    private suspend fun fetchPhases(config: TournamentConfig): SportradarResult<List<Phase>> {
        val key = cacheKey(config)

        // ── Cache read ────────────────────────────────────────────────────────
        cache?.get(key)?.let {
            telemetry.onCacheHit(key)
            return SportradarResult.Success(it)
        }
        telemetry.onCacheMiss(key)

        // ── Fetch from Sportradar ─────────────────────────────────────────────
        val fetchResult = client.fetchFixtures(config)
        if (fetchResult is SportradarResult.Failure) return fetchResult

        val body = (fetchResult as SportradarResult.Success).value
        val phases = SportradarMapper.toPhases(body, config)

        if (phases.isEmpty()) {
            return SportradarResult.Failure(SportradarError.EmptyResponse)
        }

        // ── Cache write ───────────────────────────────────────────────────────
        cache?.put(key, phases)
        if (cache != null) {
            telemetry.onCacheWrite(key, phases.size)
        }

        return SportradarResult.Success(phases)
    }

    private fun cacheKey(config: TournamentConfig): String =
        "${config.tournamentId}/${config.seasonId}"
}
