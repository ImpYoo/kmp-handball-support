package de.exhumedo.kmp.handball_support.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess

class CoachingApiClient(
    private val httpClient: HttpClient = platformHttpClient(),
) {
    suspend fun saveEvaluation(
        baseUrl: String,
        token: String,
        evaluationId: String?,
        payload: CreateCoachingEvaluationRequestDto,
    ): CoachingEvaluationResponseDto {
        val url = normalizeBaseUrl(baseUrl)
        val response = if (evaluationId == null) {
            httpClient.post("$url/api/coaching/evaluations") {
                header("Content-Type", ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(token)
                setBody(payload)
            }
        } else {
            httpClient.put("$url/api/coaching/evaluations/$evaluationId") {
                header("Content-Type", ContentType.Application.Json)
                accept(ContentType.Application.Json)
                bearerAuth(token)
                setBody(payload)
            }
        }
        return response.decode()
    }

    suspend fun getReport(
        baseUrl: String,
        token: String,
        evaluationId: String,
    ): CoachingReportResponseDto {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/coaching/evaluations/$evaluationId/report") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        return response.decode()
    }

    suspend fun listMyEvaluations(
        baseUrl: String,
        token: String,
        gameId: String? = null,
        from: String? = null,
        to: String? = null,
    ): List<CoachingEvaluationResponseDto> {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/coaching/evaluations/mine") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            gameId?.let { parameter("gameId", it) }
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }
        return response.decode()
    }

    suspend fun listAllEvaluations(
        baseUrl: String,
        token: String,
        gameId: String? = null,
        evaluatorUsername: String? = null,
        from: String? = null,
        to: String? = null,
    ): List<CoachingEvaluationResponseDto> {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/coaching/evaluations/all") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            gameId?.let { parameter("gameId", it) }
            evaluatorUsername?.let { parameter("evaluatorUsername", it) }
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }
        return response.decode()
    }

    suspend fun deleteEvaluation(
        baseUrl: String,
        token: String,
        evaluationId: String,
    ) {
        val response = httpClient.delete("${normalizeBaseUrl(baseUrl)}/api/coaching/evaluations/$evaluationId") {
            bearerAuth(token)
        }
        if (!response.status.isSuccess()) {
            throw VoteApiException(statusCode = response.status.value, message = response.bodyAsText())
        }
    }

    suspend fun getEvaluation(
        baseUrl: String,
        token: String,
        evaluationId: String,
    ): CoachingEvaluationResponseDto {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/coaching/evaluations/$evaluationId") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        return response.decode()
    }

    private fun normalizeBaseUrl(raw: String): String = raw.trim().trimEnd('/').ifBlank { "http://localhost:8080" }

    private suspend inline fun <reified T> io.ktor.client.statement.HttpResponse.decode(): T {
        if (!status.isSuccess()) {
            throw VoteApiException(
                statusCode = status.value,
                message = bodyAsText(),
            )
        }
        return body()
    }
}
