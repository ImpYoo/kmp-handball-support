package de.exhumedo.kmp.handball_support.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess

class VoteApiException(
    val statusCode: Int,
    message: String,
) : RuntimeException(message)

class VoteApiClient(
    private val httpClient: HttpClient = platformHttpClient(),
) {
    suspend fun login(baseUrl: String, username: String, password: String): TokenResponseDto {
        val response = httpClient.post("${normalizeBaseUrl(baseUrl)}/api/auth/token") {
            header("Content-Type", ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(TokenRequestDto(username = username, password = password))
        }
        return response.decode()
    }

    suspend fun getPhases(
        baseUrl: String,
        token: String? = null,
        day: Int? = null,
        month: Int? = null,
        year: Int? = null,
    ): List<PhaseResponseDto> {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/phases") {
            accept(ContentType.Application.Json)
            if (!token.isNullOrBlank()) bearerAuth(token)
            day?.let { parameter("day", it) }
            month?.let { parameter("month", it) }
            year?.let { parameter("year", it) }
        }
        return response.decode()
    }

    suspend fun getMatches(
        baseUrl: String,
        token: String? = null,
        phaseId: Int,
        day: Int? = null,
        month: Int? = null,
        year: Int? = null,
    ): List<MatchResponseDto> {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/phases/$phaseId/matches") {
            accept(ContentType.Application.Json)
            if (!token.isNullOrBlank()) bearerAuth(token)
            day?.let { parameter("day", it) }
            month?.let { parameter("month", it) }
            year?.let { parameter("year", it) }
        }
        return response.decode()
    }

    suspend fun submitVote(
        baseUrl: String,
        token: String? = null,
        payload: CreatePerformanceEvaluationRequestDto,
    ): PerformanceEvaluationResponseDto {
        val response = httpClient.post("${normalizeBaseUrl(baseUrl)}/api/performance-evaluations") {
            header("Content-Type", ContentType.Application.Json)
            accept(ContentType.Application.Json)
            if (!token.isNullOrBlank()) bearerAuth(token)
            setBody(payload)
        }
        return response.decode()
    }

    suspend fun listUsers(
        baseUrl: String,
        token: String,
    ): List<AuthUserResponseDto> {
        val response = httpClient.get("${normalizeBaseUrl(baseUrl)}/api/auth/users") {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        return response.decode()
    }

    suspend fun createUser(
        baseUrl: String,
        token: String,
        payload: CreateAuthUserRequestDto,
    ): AuthUserResponseDto {
        val response = httpClient.post("${normalizeBaseUrl(baseUrl)}/api/auth/users") {
            header("Content-Type", ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(payload)
        }
        return response.decode()
    }

    suspend fun updateUser(
        baseUrl: String,
        token: String,
        username: String,
        payload: UpdateAuthUserRequestDto,
    ): AuthUserResponseDto {
        val response = httpClient.put("${normalizeBaseUrl(baseUrl)}/api/auth/users/$username") {
            header("Content-Type", ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(payload)
        }
        return response.decode()
    }

    suspend fun deleteUser(
        baseUrl: String,
        token: String,
        username: String,
    ) {
        val response = httpClient.delete("${normalizeBaseUrl(baseUrl)}/api/auth/users/$username") {
            bearerAuth(token)
        }
        if (!response.status.isSuccess()) {
            throw VoteApiException(statusCode = response.status.value, message = response.bodyAsText())
        }
    }

    suspend fun changePassword(
        baseUrl: String,
        token: String,
        payload: ChangePasswordRequestDto,
    ): AuthUserResponseDto {
        val response = httpClient.post("${normalizeBaseUrl(baseUrl)}/api/auth/users/me/change-password") {
            header("Content-Type", ContentType.Application.Json)
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(payload)
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


