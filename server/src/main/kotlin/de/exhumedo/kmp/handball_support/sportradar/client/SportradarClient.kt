package de.exhumedo.kmp.handball_support.sportradar.client

import de.exhumedo.kmp.handball_support.sportradar.NoOpSportradarTelemetry
import de.exhumedo.kmp.handball_support.sportradar.SportradarTelemetry
import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfig
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarError
import de.exhumedo.kmp.handball_support.sportradar.error.SportradarResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.random.Random

// ── Sportradar HTTP client ────────────────────────────────────────────────────
// Interface separates the contract from the engine — swap for mock in tests or
// for a different engine (OkHttp, Darwin, Js) in KMP targets.
// ─────────────────────────────────────────────────────────────────────────────

internal interface SportradarClient {
    suspend fun fetchFixtures(config: TournamentConfig): SportradarResult<SportradarFixturesResponse>
    suspend fun fetchTournamentList(): SportradarResult<SportradarTournamentListResponse>
}

/** Retry/backoff controls and URL-building parameters for external HTTP calls. */
data class SportradarClientOptions(
    val maxAttempts: Int = 3,
    val baseBackoffMillis: Long = 200,
    val maxBackoffMillis: Long = 2_000,
    val jitterMillis: Long = 75,
    // ── Base URLs (one per league operator) ──────────────────────────────────
    val hblBaseUrl: String = "https://hbl.fmp.sportradar.com",
    val dhbBaseUrl: String = "https://dhbdata.fmp.sportradar.com",
    // ── URL path segments ─────────────────────────────────────────────────────
    val accessLevel: String = "internal",
    val language: String = "de",
    val timeZone: String = "Europe:Berlin",
    val product: String = "gismo",
    // ── Authentication ────────────────────────────────────────────────────────
    val apiKey: String? = null,
    val apiKeyHeaderName: String = "X-API-Key",
    val sendApiKeyAsQueryParam: Boolean = false,
    val apiKeyQueryParamName: String = "api_key",
)

internal class KtorSportradarClient(
    /** Inject the HttpClient from the caller so engine choice stays outside this class */
    private val httpClient: HttpClient,
    private val options: SportradarClientOptions = SportradarClientOptions(),
    private val telemetry: SportradarTelemetry = NoOpSportradarTelemetry,
) : SportradarClient {

    private val json = Json {
        ignoreUnknownKeys = true   // Sportradar responses contain many undocumented fields
        isLenient = true           // tolerates trailing commas / minor malformations
    }

    override suspend fun fetchFixtures(config: TournamentConfig): SportradarResult<SportradarFixturesResponse> =
        fetchWithRetry(buildUrl(config)) { raw ->
            val body = json.decodeFromString<SportradarFixturesResponse>(raw)
            if (body.doc.isEmpty()) SportradarResult.Failure(SportradarError.EmptyResponse)
            else SportradarResult.Success(body)
        }

    override suspend fun fetchTournamentList(): SportradarResult<SportradarTournamentListResponse> =
        fetchWithRetry(buildTournamentListUrl()) { raw ->
            val body = json.decodeFromString<SportradarTournamentListResponse>(raw)
            if (body.doc.isEmpty()) SportradarResult.Failure(SportradarError.EmptyResponse)
            else SportradarResult.Success(body)
        }

    // ── Shared retry / auth / backoff logic ──────────────────────────────────
    // All public fetch methods delegate here; only URL and response parsing differ.

    private suspend fun <T> fetchWithRetry(
        url: String,
        parse: (String) -> SportradarResult<T>,
    ): SportradarResult<T> {
        var attempt = 1
        while (attempt <= options.maxAttempts) {
            telemetry.onRequestStart(url, attempt)

            val result: SportradarResult<T> = try {
                val response = httpClient.get(url) {
                    if (!options.sendApiKeyAsQueryParam && options.apiKey != null) {
                        header(options.apiKeyHeaderName, options.apiKey)
                    }
                }
                if (!response.status.isSuccess()) {
                    SportradarResult.Failure(SportradarError.HttpError(response.status.value))
                } else {
                    val raw = response.bodyAsText()
                    try {
                        parse(raw)
                    } catch (e: Exception) {
                        SportradarResult.Failure(SportradarError.ParseError(e))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                SportradarResult.Failure(SportradarError.NetworkError(e))
            }

            when (result) {
                is SportradarResult.Success -> {
                    telemetry.onRequestSuccess(url, attempt)
                    return result
                }
                is SportradarResult.Failure -> {
                    val retryable = isRetryable(result.error)
                    val hasNextAttempt = attempt < options.maxAttempts
                    if (!retryable || !hasNextAttempt) {
                        telemetry.onRequestFailure(url, attempt, result.error)
                        return result
                    }
                    val backoff = calculateBackoff(attempt)
                    telemetry.onRequestRetry(url, attempt, backoff, result.error)
                    delay(backoff)
                    attempt += 1
                }
            }
        }
        return SportradarResult.Failure(SportradarError.NetworkError(IllegalStateException("retry loop exhausted")))
    }

    private fun isRetryable(error: SportradarError): Boolean = when (error) {
        is SportradarError.NetworkError -> true
        is SportradarError.HttpError -> error.statusCode in RETRYABLE_STATUS_CODES
        is SportradarError.EmptyResponse -> false
        is SportradarError.ParseError -> false
    }

    private fun calculateBackoff(attempt: Int): Long {
        val exponent = (attempt - 1).coerceAtLeast(0)
        val base = options.baseBackoffMillis * (1L shl exponent)
        val jitter = if (options.jitterMillis > 0) Random.nextLong(options.jitterMillis + 1) else 0
        return (base + jitter).coerceAtMost(options.maxBackoffMillis)
    }

    private fun buildUrl(config: TournamentConfig): String {
        val base = if (config.isHbl) options.hblBaseUrl else options.dhbBaseUrl
        val path = "/feeds/${options.accessLevel}/${options.language}/${options.timeZone}/${options.product}/fixtures"
        var url = "$base$path/${config.tournamentId}/${config.seasonId}"
        if (options.sendApiKeyAsQueryParam && options.apiKey != null) {
            url += "?${options.apiKeyQueryParamName}=${options.apiKey}"
        }
        return url
    }

    /** Tournament list URL — no accessLevel segment, uses "tournaments" (plural). */
    private fun buildTournamentListUrl(): String {
        val path = "/feeds/${options.language}/${options.timeZone}/${options.product}/tournaments"
        var url = "${options.hblBaseUrl}$path"
        if (options.sendApiKeyAsQueryParam && options.apiKey != null) {
            url += "?${options.apiKeyQueryParamName}=${options.apiKey}"
        }
        return url
    }

    companion object {
        private val RETRYABLE_STATUS_CODES = setOf(
            HttpStatusCode.TooManyRequests.value,
            HttpStatusCode.InternalServerError.value,
            HttpStatusCode.BadGateway.value,
            HttpStatusCode.ServiceUnavailable.value,
            HttpStatusCode.GatewayTimeout.value,
        )
    }
}

