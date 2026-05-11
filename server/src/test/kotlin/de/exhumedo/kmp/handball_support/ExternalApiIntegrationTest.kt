package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.config.ExternalApiConfig
import de.exhumedo.kmp.handball_support.integration.KtorExternalApiGateway
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that call the real Sportradar FMP endpoint.
 *
 * Values are resolved from (in priority order):
 *  1. System environment variables
 *  2. `server/.env.test` file  ← create this locally, it is git-ignored
 *
 * Required keys:
 *   EXTERNAL_API_ENABLED=true
 *   EXTERNAL_API_KEY=<your-api-key>
 *
 * Optional keys (with defaults):
 *   EXTERNAL_API_BASE_URL=https://hbl.fmp.sportradar.com
 *   EXTERNAL_API_LANGUAGE=de
 *   EXTERNAL_API_TIMEZONE=Europe:Berlin
 *   EXTERNAL_API_PRODUCT=gismo
 *   EXTERNAL_API_KEY_AS_QUERY_PARAM=false
 *   EXTERNAL_API_KEY_QUERY_PARAM_NAME=api_key
 *   EXTERNAL_API_PHASE_ID=<phase-id>          (for getStandings)
 *   EXTERNAL_API_TOURNAMENT_ID=<tourney-id>   (for getFixtures / getTeamInfo)
 *   EXTERNAL_API_SEASON_ID=<season-id>        (for getFixtures / getTeamInfo)
 *   EXTERNAL_API_TEAM_ID=<team-id>            (for getTeamInfo)
 *
 * ── IntelliJ ────────────────────────────────────────────────────────────────
 * Create  server/.env.test  (already in .gitignore):
 *
 *   EXTERNAL_API_ENABLED=true
 *   EXTERNAL_API_KEY=your-key-here
 *   EXTERNAL_API_KEY_AS_QUERY_PARAM=true
 *
 * Then just right-click the test class → Run.
 *
 * ── CLI ─────────────────────────────────────────────────────────────────────
 *   ./gradlew :server:test --tests "*.ExternalApiIntegrationTest" \
 *     -PEXTERNAL_API_ENABLED=true   # or use the .env.test file
 */
class ExternalApiIntegrationTest {

    // ----- env resolution (env var → .env.test file → default) -----

    private val dotEnv: Map<String, String> by lazy { loadDotEnvTest() }

    private fun env(key: String): String? = System.getenv(key) ?: dotEnv[key]

    private fun loadDotEnvTest(): Map<String, String> {
        // Gradle test executor CWD = <project>/server/  →  ".env.test" resolves to server/.env.test
        // IntelliJ run config CWD  = <project>/server/  →  same
        // Fallbacks cover edge cases (e.g. running from project root directly)
        val candidates = listOf(
            File(".env.test"),              // server/.env.test  (CWD = server/)
            File("server/.env.test"),       // server/.env.test  (CWD = project root)
            File("../.env.test"),           // safety net
        )
        val file = candidates.firstOrNull { it.exists() }
        if (file == null) {
            val checked = candidates.map { it.absolutePath }
            println("[Integration] No .env.test file found. Checked: $checked")
            println("[Integration] Create server/.env.test based on server/.env.test.example to enable real-endpoint tests.")
            return emptyMap()
        }
        println("[Integration] Loading config from ${file.absolutePath}")
        return file.readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
                val idx = trimmed.indexOf('=').takeIf { it > 0 } ?: return@mapNotNull null
                val key = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
                key to value
            }.toMap()
    }

    private val enabled get() = env("EXTERNAL_API_ENABLED")?.toBooleanStrictOrNull() ?: false
    private val baseUrl get() = env("EXTERNAL_API_BASE_URL") ?: "https://hbl.fmp.sportradar.com"
    private val apiKey get() = env("EXTERNAL_API_KEY")
    private val language get() = env("EXTERNAL_API_LANGUAGE") ?: "de"
    private val timeZone get() = env("EXTERNAL_API_TIMEZONE") ?: "Europe:Berlin"
    private val product get() = env("EXTERNAL_API_PRODUCT") ?: "gismo"
    private val sendAsQueryParam get() = env("EXTERNAL_API_KEY_AS_QUERY_PARAM")?.toBooleanStrictOrNull() ?: false
    private val queryParamName get() = env("EXTERNAL_API_KEY_QUERY_PARAM_NAME") ?: "api_key"

    private fun assumeIntegrationEnabled() {
        assumeTrue(
            "Skipping real-endpoint test: set EXTERNAL_API_ENABLED=true and EXTERNAL_API_KEY to run",
            enabled && apiKey != null,
        )
    }

    private fun buildGateway(): KtorExternalApiGateway {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
            }
        }
        return KtorExternalApiGateway(
            httpClient = client,
            config = ExternalApiConfig(
                enabled = true,
                baseUrl = baseUrl,
                language = language,
                timeZone = timeZone,
                product = product,
                apiKey = apiKey,
                sendApiKeyAsQueryParam = sendAsQueryParam,
                apiKeyQueryParamName = queryParamName,
            ),
        )
    }

    // ----- tests -----

    @Test
    fun getTournamentReturnsData() {
        assumeIntegrationEnabled()
        val gateway = buildGateway()

        val feed = runBlocking { gateway.getTournament() }

        println("[Integration] getTournament generatedAt=${feed.meta.generatedAt} cacheMs=${feed.meta.minCacheMillis}")
        assertNotNull(feed.payload)
        assertTrue(feed.payload.isNotEmpty(), "Tournament payload must not be empty")
    }

    @Test
    fun getStandingsReturnsData() {
        assumeIntegrationEnabled()
        val phaseId = env("EXTERNAL_API_PHASE_ID")
            ?: run {
                println("[Integration] Skipping getStandings: set EXTERNAL_API_PHASE_ID")
                assumeTrue(false)
                return
            }

        val gateway = buildGateway()
        val feed = runBlocking { gateway.getStandings(phaseId) }

        println("[Integration] getStandings phaseId=$phaseId generatedAt=${feed.meta.generatedAt}")
        assertNotNull(feed.payload)
    }

    @Test
    fun getFixturesReturnsData() {
        assumeIntegrationEnabled()
        val tournamentId = env("EXTERNAL_API_TOURNAMENT_ID")
        val seasonId = env("EXTERNAL_API_SEASON_ID")
        if (tournamentId == null || seasonId == null) {
            println("[Integration] Skipping getFixtures: set EXTERNAL_API_TOURNAMENT_ID and EXTERNAL_API_SEASON_ID")
            assumeTrue(false)
            return
        }

        val gateway = buildGateway()
        val feed = runBlocking { gateway.getFixtures(tournamentId, seasonId) }

        println("[Integration] getFixtures t=$tournamentId s=$seasonId cacheMs=${feed.meta.minCacheMillis}")
        assertNotNull(feed.payload)
    }

    @Test
    fun getTeamInfoReturnsData() {
        assumeIntegrationEnabled()
        val tournamentId = env("EXTERNAL_API_TOURNAMENT_ID")
        val seasonId = env("EXTERNAL_API_SEASON_ID")
        val teamId = env("EXTERNAL_API_TEAM_ID")
        if (tournamentId == null || seasonId == null || teamId == null) {
            println("[Integration] Skipping getTeamInfo: set EXTERNAL_API_TOURNAMENT_ID, EXTERNAL_API_SEASON_ID, EXTERNAL_API_TEAM_ID")
            assumeTrue(false)
            return
        }

        val gateway = buildGateway()
        val feed = runBlocking { gateway.getTeamInfo(tournamentId, seasonId, teamId) }

        println("[Integration] getTeamInfo team=$teamId generatedAt=${feed.meta.generatedAt}")
        assertNotNull(feed.payload)
    }
}

