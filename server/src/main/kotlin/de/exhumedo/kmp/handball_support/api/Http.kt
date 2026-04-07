package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.ProblemDto
import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import de.exhumedo.kmp.handball_support.persistence.DuplicateGameEvaluationException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json

/**
 * Configures JSON serialization and problem-style error handling.
 */
fun Application.configureHttp() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    install(StatusPages) {
        exception<DuplicateGameEvaluationException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.Conflict,
                title = "Conflict",
                detail = cause.message ?: "An evaluation for this game already exists.",
            )
        }
        exception<DomainException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.BadRequest,
                title = "Domain Validation Failed",
                detail = cause.message ?: "The request violates domain rules.",
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.BadRequest,
                title = "Invalid Request",
                detail = cause.message ?: "The request is invalid.",
            )
        }
        exception<Throwable> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.InternalServerError,
                title = "Internal Server Error",
                detail = cause.message ?: "An unexpected error occurred.",
            )
        }
    }
}

internal suspend fun io.ktor.server.application.ApplicationCall.respondProblem(
    status: HttpStatusCode,
    title: String,
    detail: String,
) {
    response.header(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString())
    respond(
        status = status,
        message = ProblemDto(
            type = "about:blank",
            title = title,
            status = status.value,
            detail = detail,
            instance = request.path(),
        ),
    )
}
