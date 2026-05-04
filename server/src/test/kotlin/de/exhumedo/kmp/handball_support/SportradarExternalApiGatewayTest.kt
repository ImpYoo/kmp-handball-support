package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.config.ExternalApiConfig
import de.exhumedo.kmp.handball_support.integration.KtorExternalApiGateway
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SportradarExternalApiGatewayTest {

    @Test
    fun getTournamentReturnsValidFeed() {
        val mockEngine = MockEngine { request ->
            assertEquals(true, request.url.toString().contains("/feeds/en/Europe:Berlin/gismo/tournament/"))
            respond(
                content = """{
                    "Doc": {
                        "_dob": "2026-04-15T10:30:00Z",
                        "_maxage": "300000",
                        "Data": {"Tournaments": []}
                    }
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val gateway = KtorExternalApiGateway(
            httpClient = client,
            config = ExternalApiConfig(
                enabled = true,
                baseUrl = "https://hbl.fmp.sportradar.com",
                language = "en",
                timeZone = "Europe:Berlin",
                product = "gismo",
            ),
        )

        val feed = runBlocking { gateway.getTournament() }
        assertNotNull(feed.payload)
        assertEquals("2026-04-15T10:30:00Z", feed.meta.generatedAt)
        assertEquals(300000L, feed.meta.minCacheMillis)
    }

    @Test
    fun getStandingsReturnsValidFeed() {
        val mockEngine = MockEngine { request ->
            assertEquals(true, request.url.toString().contains("/feeds/en/Europe:Berlin/gismo/standings/PHASE-123"))
            respond(
                content = """{
                    "Doc": {
                        "_dob": "2026-04-15T10:30:00Z",
                        "_maxage": "300000",
                        "Data": {}
                    }
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val gateway = KtorExternalApiGateway(
            httpClient = client,
            config = ExternalApiConfig(
                enabled = true,
                baseUrl = "https://hbl.fmp.sportradar.com",
                language = "en",
                timeZone = "Europe:Berlin",
                product = "gismo",
            ),
        )

        val feed = runBlocking { gateway.getStandings("PHASE-123") }
        assertNotNull(feed.payload)
        assertEquals("2026-04-15T10:30:00Z", feed.meta.generatedAt)
    }

    @Test
    fun getFixturesReturnsValidFeed() {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            assertEquals(true, url.contains("/feeds/en/Europe:Berlin/gismo/fixtures/921/33765"))
            respond(
                content = """{
                    "Doc": {
                        "_dob": "2026-04-15T10:30:00Z",
                        "_maxage": "600000",
                        "Data": {"Tournament": {}, "Matchdays": []}
                    }
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val gateway = KtorExternalApiGateway(
            httpClient = client,
            config = ExternalApiConfig(
                enabled = true,
                baseUrl = "https://hbl.fmp.sportradar.com",
                language = "en",
                timeZone = "Europe:Berlin",
                product = "gismo",
            ),
        )

        val feed = runBlocking { gateway.getFixtures("921", "33765") }
        assertNotNull(feed.payload)
        assertEquals(600000L, feed.meta.minCacheMillis)
    }

    @Test
    fun getTeamInfoReturnsValidFeed() {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            assertEquals(true, url.contains("/feeds/en/Europe:Berlin/gismo/team_info/921/33765/TEAM-001"))
            respond(
                content = """{
                    "Doc": {
                        "_dob": "2026-04-15T10:30:00Z",
                        "_maxage": "300000",
                        "Data": {"Team_info": {}}
                    }
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val gateway = KtorExternalApiGateway(
            httpClient = client,
            config = ExternalApiConfig(
                enabled = true,
                baseUrl = "https://hbl.fmp.sportradar.com",
                language = "en",
                timeZone = "Europe:Berlin",
                product = "gismo",
            ),
        )

        val feed = runBlocking { gateway.getTeamInfo("921", "33765", "TEAM-001") }
        assertNotNull(feed.payload)
        assertEquals("2026-04-15T10:30:00Z", feed.meta.generatedAt)
    }

    @Test
    fun sendsApiKeyAsQueryParameterWhenConfigured() {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            assertEquals(true, url.contains("api_key=secret-key-123"))
            respond(
                content = """{"Doc": {"_dob": "2026-04-15T10:30:00Z", "_maxage": "300000"}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val gateway = KtorExternalApiGateway(
            httpClient = client,
            config = ExternalApiConfig(
                enabled = true,
                baseUrl = "https://hbl.fmp.sportradar.com",
                language = "en",
                timeZone = "Europe:Berlin",
                product = "gismo",
                apiKey = "secret-key-123",
                sendApiKeyAsQueryParam = true,
                apiKeyQueryParamName = "api_key",
            ),
        )

        val feed = runBlocking { gateway.getTournament() }
        assertNotNull(feed.payload)
    }
}

