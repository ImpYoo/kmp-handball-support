package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.nuliga.NuLigaApplicationService
import de.exhumedo.kmp.handball_support.nuliga.NuLigaClient
import de.exhumedo.kmp.handball_support.nuliga.NuLigaError
import de.exhumedo.kmp.handball_support.nuliga.NuLigaMatchPerson
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResolvedMatch
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResult
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResolvedTeam
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NuLigaRoutingTest {

    private val sampleUrl = "https://hbde-live.liga.nu/nuScoreLive/#/groups/489670/meetings/8278820"

    private fun fakeClient(result: NuLigaResult<NuLigaResolvedMatch>): NuLigaClient =
        object : NuLigaClient {
            override suspend fun resolveMeeting(meetingId: String): NuLigaResult<NuLigaResolvedMatch> = result
        }

    private fun resolvedFixture() = NuLigaResolvedMatch(
        sourceMatchId = "8278820",
        scheduledAt = Instant.parse("2026-09-13T14:00:00Z"),
        isLive = false,
        periods = 2,
        minutesPerPeriod = 30,
        competitionName = "Gruppe 489670",
        venueName = "Oldenburg, EWE Arena",
        home = NuLigaResolvedTeam(
            name = "TvdH Oldenburg",
            logoUrl = null,
            players = emptyList(),
            officials = emptyList(),
        ),
        away = NuLigaResolvedTeam(
            name = "TV Cloppenburg",
            logoUrl = null,
            players = emptyList(),
            officials = emptyList(),
        ),
        referees = listOf(
            NuLigaMatchPerson("r-1", "Ref", "Eins", "REFEREE", 1),
            NuLigaMatchPerson("r-2", "Ref", "Zwei", "REFEREE", 2),
        ),
        tableOfficials = emptyList(),
        supervisors = emptyList(),
    )

    @Test
    fun `resolution requires authentication`() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
                nuLigaClient = fakeClient(NuLigaResult.Success(resolvedFixture())),
            )
        }

        val response = client.post("/api/nuliga/match-resolutions") {
            header(HttpHeaders.Accept, ContentType.Application.Json)
            setBody("""{"url":"$sampleUrl"}""")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `resolves a pasted nuLiga url into teams, rosters and officials`() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
                nuLigaClient = fakeClient(NuLigaResult.Success(resolvedFixture())),
            )
        }

        val token = issueToken(username = "admin", password = "AdminPass123!")
        val response = client.post("/api/nuliga/match-resolutions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"url":"$sampleUrl"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"TvdH Oldenburg\""), body)
        assertTrue(body.contains("\"REFEREE\""), body)
        assertTrue(body.contains("\"sourceMatchId\":\"8278820\""), body)
    }

    @Test
    fun `rejects a foreign url with a 400 problem`() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
                nuLigaClient = fakeClient(NuLigaResult.Success(resolvedFixture())),
            )
        }

        val token = issueToken(username = "admin", password = "AdminPass123!")
        val response = client.post("/api/nuliga/match-resolutions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"url":"https://www.handball.net/match/415227"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("nuLiga"))
    }

    @Test
    fun `maps an upstream failure to a 502 problem`() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
                nuLigaClient = fakeClient(NuLigaResult.Failure(NuLigaError.HttpError(404))),
            )
        }

        val token = issueToken(username = "admin", password = "AdminPass123!")
        val response = client.post("/api/nuliga/match-resolutions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"url":"$sampleUrl"}""")
        }

        assertEquals(HttpStatusCode.BadGateway, response.status)
        assertTrue(response.bodyAsText().contains("404"))
    }

    @Test
    fun `rejects a body without a url`() = testApplication {
        application {
            module(
                appConfig = createTestAppConfig(),
                authUserStore = createTestAuthUserStore(),
                nuLigaClient = fakeClient(NuLigaResult.Success(resolvedFixture())),
            )
        }

        val token = issueToken(username = "admin", password = "AdminPass123!")
        val response = client.post("/api/nuliga/match-resolutions") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"wrong":"field"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}