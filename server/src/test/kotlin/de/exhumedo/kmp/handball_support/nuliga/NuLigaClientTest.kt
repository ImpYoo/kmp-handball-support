package de.exhumedo.kmp.handball_support.nuliga

import de.exhumedo.kmp.handball_support.nuliga.client.KtorNuLigaClient
import de.exhumedo.kmp.handball_support.nuliga.NuLigaClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Client and URL tests against **payloads captured from the live nuLiga system**, stored under
 * `src/test/resources/fixtures` — carried over from the predecessor's adapter tests.
 *
 * These are not hand-written mocks: the JSON is exactly what nuScoreLive returned, so a mapping
 * that passes here is known to work against the real shape. If the source changes its payload,
 * these tests fail and point at the client — which is the whole point of captured data.
 *
 * The meeting fixture is pinned to nuLiga meeting 8278820 (TvdH Oldenburg vs TV Cloppenburg).
 */
class NuLigaClientTest {

    private val meetingVersion = "eef250ea-0d91-46e8-8a9d-55f520b3c7b1"

    private fun fixture(name: String): String =
        File("src/test/resources/fixtures/$name").readText()

    private fun fixtureClient(): NuLigaClient {
        val byUrl = mapOf(
            "meeting/8278820/time/" to fixture("nuliga-meeting.json"),
            "players/8278820/versions/$meetingVersion" to fixture("nuliga-players.json"),
            "officials/8278820/versions/$meetingVersion" to fixture("nuliga-officials.json"),
            "fedOfficials/8278820/versions/$meetingVersion" to fixture("nuliga-fedofficials.json"),
        )
        return KtorNuLigaClient(
            http = jsonEngine(byUrl),
            // Fixed clock so the 15-second bucket is deterministic.
            clock = { Instant.ofEpochSecond(1_789_677_105) },
        )
    }

    private fun jsonEngine(byUrl: Map<String, String>): HttpClient =
        HttpClient(
            MockEngine { request ->
                val match = byUrl.entries.firstOrNull { request.url.toString().contains(it.key) }
                    ?: return@MockEngine respondError(HttpStatusCode.NotFound)
                respond(
                    content = match.value,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )

    // ── URL handling (NuLigaApplicationService) ───────────────────────────────

    @Test
    fun `accepts only liga nu urls`() {
        val service = NuLigaApplicationService(fixtureClient())
        assertTrue(service.accepts("https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/8278820"))
        assertTrue(service.accepts("https://hbde-live.liga.nu/nuScoreLiveRestBackend/api/1/meeting/8278820"))
        assertTrue(!service.accepts("https://www.handball.net/match/415227"))
        assertTrue(!service.accepts("not a url"))
    }

    @Test
    fun `reads the meeting id out of the url fragment`() {
        val service = NuLigaApplicationService(fixtureClient())
        // The public URL is a hash route: /nuScoreLive/#/groups/{groupId}/meetings/{meetingId}.
        assertEquals(
            "8278820",
            service.meetingIdFrom("https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/8278820"),
        )
    }

    @Test
    fun `rejects foreign urls with a client-side message`() {
        val service = NuLigaApplicationService(fixtureClient())
        val e = assertFailsWith<NuLigaApplicationService.InvalidMatchUrlException> {
            service.meetingIdFrom("https://www.handball.net/match/415227")
        }
        assertTrue(e.message!!.contains("not a nuLiga URL"))
    }

    @Test
    fun `rejects non-numeric meeting ids`() {
        val service = NuLigaApplicationService(fixtureClient())
        val e = assertFailsWith<NuLigaApplicationService.InvalidMatchUrlException> {
            service.meetingIdFrom("https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/not-a-number")
        }
        assertTrue(e.message!!.contains("numeric"))
    }

    // ── Resolution against captured payloads ──────────────────────────────────

    @Test
    fun `resolves teams, hall and config from the meeting payload`() = kotlinx.coroutines.runBlocking {
        val match = fixtureClient().resolveMeeting("8278820")
            as NuLigaResult.Success

        assertEquals("8278820", match.value.sourceMatchId)
        assertEquals("TvdH Oldenburg", match.value.home.name)
        assertEquals("TV Cloppenburg", match.value.away.name)
        assertEquals("Oldenburg, EWE Arena", match.value.venueName, "hall name is trimmed")
        assertEquals("Gruppe 489670", match.value.competitionName)
        assertEquals(30, match.value.minutesPerPeriod, "halftimeLength is published per match")
        assertEquals(2, match.value.periods)
    }

    @Test
    fun `normalizes the non-rfc3339 timestamp to an instant`() = kotlinx.coroutines.runBlocking {
        val match = fixtureClient().resolveMeeting("8278820")
            as NuLigaResult.Success
        // The source sends 2026-09-13T14:00:00.000+0000 (no colon in the offset), which is not
        // valid RFC 3339 and must still normalize to a proper instant.
        assertEquals(Instant.parse("2026-09-13T14:00:00Z"), match.value.scheduledAt)
    }

    @Test
    fun `splits players by team using the teamHome flag`() = kotlinx.coroutines.runBlocking {
        val match = fixtureClient().resolveMeeting("8278820")
            as NuLigaResult.Success

        // One flat players payload covers both teams; teamHome decides the side.
        assertTrue(match.value.home.players.isNotEmpty(), "home roster")
        assertTrue(match.value.away.players.isNotEmpty(), "away roster")

        val heyen = match.value.home.players.single { it.lastName == "Heyen" }
        assertEquals("Johann", heyen.firstName)
        assertEquals(19, heyen.number)

        // Every player is on exactly one side.
        val homeIds = match.value.home.players.map { it.id }.toSet()
        val awayIds = match.value.away.players.map { it.id }.toSet()
        assertTrue(homeIds.intersect(awayIds).isEmpty(), "a player cannot be on both sides")
    }

    @Test
    fun `maps the ihf letter on team officials`() = kotlinx.coroutines.runBlocking {
        val match = fixtureClient().resolveMeeting("8278820")
            as NuLigaResult.Success
        val homeOfficials = match.value.home.officials
        assertTrue(homeOfficials.isNotEmpty())
        // nuLiga publishes the IHF designation (A–E) in the `nr` field for officials.
        assertTrue(
            homeOfficials.any { it.badge == "A" },
            "expected an A official, got ${homeOfficials.map { it.badge }}",
        )
        assertTrue(homeOfficials.all { it.badge != null })
    }

    @Test
    fun `classifies federation people by personType`() = kotlinx.coroutines.runBlocking {
        val match = fixtureClient().resolveMeeting("8278820")
            as NuLigaResult.Success

        assertTrue(match.value.referees.isNotEmpty(), "two referees in the fixture")
        assertTrue(match.value.referees.all { it.roleCode == "REFEREE" })
        assertTrue(match.value.tableOfficials.all { it.roleCode == "TIMEKEEPER" || it.roleCode == "SECRETARY" })
        // Referee observer belongs to supervisors, not to the referee list.
        assertTrue(match.value.referees.none { it.roleCode == "REFEREE_OBSERVER" })
    }

    @Test
    fun `pins all roster calls to the same version`() = kotlinx.coroutines.runBlocking {
        // The fixture engine only knows the four version-pinned URLs; an unpinned /time/ call
        // for a roster would 404 and the resolution would fail. Passing proves pinning.
        val match = fixtureClient().resolveMeeting("8278820")
        assertTrue(match is NuLigaResult.Success)
    }

    // ── Error paths ────────────────────────────────────────────────────────────

    @Test
    fun `reports upstream http errors`() = kotlinx.coroutines.runBlocking {
        val client = KtorNuLigaClient(
            http = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }),
            clock = { Instant.ofEpochSecond(1_789_677_105) },
        )
        val result = client.resolveMeeting("8278820")
        assertTrue(result is NuLigaResult.Failure)
        assertEquals(404, (result as NuLigaResult.Failure).error.let { (it as NuLigaError.HttpError).statusCode })
    }

    @Test
    fun `reports unparseable payloads`() = kotlinx.coroutines.runBlocking {
        val client = KtorNuLigaClient(
            http = HttpClient(MockEngine { respond("this is not json") }),
            clock = { Instant.ofEpochSecond(1_789_677_105) },
        )
        val result = client.resolveMeeting("8278820")
        assertTrue((result as NuLigaResult.Failure).error is NuLigaError.ParseError)
    }

    @Test
    fun `reports a missing version instead of guessing rosters`() = kotlinx.coroutines.runBlocking {
        // A meeting without versionUid cannot be read consistently; the client refuses.
        val client = KtorNuLigaClient(
            http = jsonEngine(mapOf("meeting/1/time/" to """{"meetingID":"1"}""")),
            clock = { Instant.ofEpochSecond(1_789_677_105) },
        )
        val result = client.resolveMeeting("1")
        assertTrue((result as NuLigaResult.Failure).error is NuLigaError.MissingVersion)
        assertNull(null) // keeps the import honest if assertions above change
    }
}