package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.sportradar.cache.FixturesCache
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarClient
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarDocData
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarDocEntry
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarFixturesResponse
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarMatchDay
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarPhase
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarTimestamp
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarTournament
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarTournamentData
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarTournamentDocEntry
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarTournamentEntry
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarTournamentListResponse
import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfig
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarResult
import de.exhumedo.kmp.handball_support.sportradar.repository.SportradarPhaseRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SportradarPhaseRepositoryRefreshTest {

    @Test
    fun refreshCachesClearsAndWarmsTournamentAndFixtureCaches() = runBlocking {
        val client = CountingClient()
        val repo = SportradarPhaseRepository(
            client = client,
            configs = listOf(TournamentConfig(tournamentId = 921, seasonId = 33765, isHbl = true, label = "HBL")),
            cache = FixturesCache(ttlMillis = 60_000),
        )

        // Initial call warms fixtures cache.
        val first = repo.getAllPhases()
        assertTrue(first is SportradarResult.Success)
        assertEquals(1, client.fixturesCalls)
        assertEquals(0, client.tournamentCalls)

        // Second call should hit fixtures cache.
        val second = repo.getAllPhases()
        assertTrue(second is SportradarResult.Success)
        assertEquals(1, client.fixturesCalls)

        // Refresh clears caches and eagerly warms tournament + configured fixtures.
        val refreshed = repo.refreshCaches()
        assertTrue(refreshed is SportradarResult.Success)
        assertEquals(1, refreshed.value.tournamentCount)
        assertEquals(1, refreshed.value.configuredSeasonCount)
        assertEquals(1, refreshed.value.phaseCount)
        assertEquals(1, client.tournamentCalls)
        assertEquals(2, client.fixturesCalls)

        // After refresh, first read should still hit cache warmed during refresh.
        val afterRefresh = repo.getAllPhases()
        assertTrue(afterRefresh is SportradarResult.Success)
        assertEquals(2, client.fixturesCalls)
    }

    private class CountingClient : SportradarClient {
        var fixturesCalls: Int = 0
        var tournamentCalls: Int = 0

        override suspend fun fetchFixtures(config: TournamentConfig): SportradarResult<SportradarFixturesResponse> {
            fixturesCalls += 1
            return SportradarResult.Success(
                SportradarFixturesResponse(
                    doc = listOf(
                        SportradarDocEntry(
                            data = SportradarDocData(
                                tournament = SportradarTournament(
                                    phases = listOf(
                                        SportradarPhase(
                                            id = 1,
                                            name = "Regular Season",
                                            matchdays = listOf(
                                                SportradarMatchDay(
                                                    id = 10,
                                                    currentMatchday = true,
                                                    title = "MD1",
                                                    startDate = SportradarTimestamp(1),
                                                    endDate = SportradarTimestamp(2),
                                                    matches = emptyList(),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        override suspend fun fetchTournamentList(): SportradarResult<SportradarTournamentListResponse> {
            tournamentCalls += 1
            return SportradarResult.Success(
                SportradarTournamentListResponse(
                    doc = listOf(
                        SportradarTournamentDocEntry(
                            data = SportradarTournamentData(
                                tournaments = listOf(
                                    SportradarTournamentEntry(
                                        id = 921,
                                        name = "HBL",
                                        seasons = emptyList(),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
    }
}

