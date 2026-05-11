package de.exhumedo.kmp.handball_support.integration

import de.exhumedo.kmp.handball_support.application.ExternalApiException
import de.exhumedo.kmp.handball_support.application.ExternalApiGateway
import de.exhumedo.kmp.handball_support.application.SportradarFeedMetaDto
import de.exhumedo.kmp.handball_support.application.SportradarFixturesFeedDto
import de.exhumedo.kmp.handball_support.application.SportradarStandingsFeedDto
import de.exhumedo.kmp.handball_support.application.SportradarTeamInfoFeedDto
import de.exhumedo.kmp.handball_support.application.SportradarTournamentFeedDto
import de.exhumedo.kmp.handball_support.config.ExternalApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Ktor HTTP adapter for outbound external API GET calls.
 */
class KtorExternalApiGateway(
    private val httpClient: HttpClient,
    private val config: ExternalApiConfig,
) : ExternalApiGateway {

    override suspend fun getTournament(): SportradarTournamentFeedDto {
        val payload = execute("getTournament") {
            httpClient.get("${routeBase()}/tournament/") {
                applyAuth(this)
            }.body<JsonObject>()
        }

        return SportradarTournamentFeedDto(
            meta = extractMeta(payload),
            payload = payload,
        )
    }

    override suspend fun getStandings(phaseId: String): SportradarStandingsFeedDto {
        val payload = execute("getStandings") {
            httpClient.get("${routeBase()}/standings/$phaseId") {
                applyAuth(this)
            }.body<JsonObject>()
        }

        return SportradarStandingsFeedDto(
            meta = extractMeta(payload),
            payload = payload,
        )
    }

    override suspend fun getFixtures(
        tournamentId: String,
        seasonId: String,
    ): SportradarFixturesFeedDto {
        val payload = execute("getFixtures") {
            httpClient.get("${routeBase()}/fixtures/$tournamentId/$seasonId") {
                applyAuth(this)
            }.body<JsonObject>()
        }

        return SportradarFixturesFeedDto(
            meta = extractMeta(payload),
            payload = payload,
        )
    }

    override suspend fun getTeamInfo(
        tournamentId: String,
        seasonId: String,
        teamId: String,
    ): SportradarTeamInfoFeedDto {
        val payload = execute("getTeamInfo") {
            httpClient.get("${routeBase()}/team_info/$tournamentId/$seasonId/$teamId") {
                applyAuth(this)
            }.body<JsonObject>()
        }

        return SportradarTeamInfoFeedDto(
            meta = extractMeta(payload),
            payload = payload,
        )
    }

    private suspend fun <T> execute(
        operation: String,
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (cause: HttpRequestTimeoutException) {
            throw ExternalApiException("External API timeout while calling '$operation'.", cause)
        } catch (cause: ResponseException) {
            throw ExternalApiException(
                "External API responded with ${cause.response.status.value} during '$operation'.",
                cause,
            )
        } catch (cause: IOException) {
            throw ExternalApiException("External API I/O error while calling '$operation'.", cause)
        }
    }

    private fun routeBase(): String {
        return "${config.baseUrl.trimEnd('/')}/feeds/${config.accessLevel}/${config.language}/${config.timeZone}/${config.product}"
    }

    private fun applyAuth(builder: io.ktor.client.request.HttpRequestBuilder) {
        val key = config.apiKey ?: return
        if (config.sendApiKeyAsQueryParam) {
            builder.parameter(config.apiKeyQueryParamName, key)
            return
        }

        builder.header(config.apiKeyHeaderName, key)
    }

    private fun extractMeta(payload: JsonObject): SportradarFeedMetaDto {
        val docObject = payload.lookupObject("doc")
            ?: payload.lookupObject("Doc")
            ?: payload

        val generatedAt = docObject.lookupString("_dob")
        val minCacheMillis = docObject.lookupLong("_maxage")

        return SportradarFeedMetaDto(
            generatedAt = generatedAt,
            minCacheMillis = minCacheMillis,
        )
    }

    private fun JsonObject.lookupObject(key: String): JsonObject? {
        return this[key] as? JsonObject
    }

    private fun JsonObject.lookupString(key: String): String? {
        val value = this[key] ?: return null
        return (value as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonObject.lookupLong(key: String): Long? {
        val value = this[key] ?: return null
        return (value as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
    }

    private val JsonPrimitive.contentOrNull: String?
        get() = if (isString || content != "null") content else null
}

