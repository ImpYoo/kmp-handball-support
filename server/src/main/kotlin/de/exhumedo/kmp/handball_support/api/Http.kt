package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.ProblemDto
import de.exhumedo.kmp.handball_support.auth.AuthUserAlreadyExistsException
import de.exhumedo.kmp.handball_support.auth.AuthenticationThrottledException
import de.exhumedo.kmp.handball_support.auth.AuthUserNotFoundException
import de.exhumedo.kmp.handball_support.auth.LastEnabledAdminRemovalException
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.domain.rating.exception.DomainException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.header
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Configures JSON serialization and problem-style error handling.
 */
fun Application.configureHttp(appConfig: AppConfig) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }

    if (appConfig.http.corsAllowedOrigins.isNotEmpty()) {
        install(CORS) {
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Accept)
            allowHeader(HttpHeaders.Origin)
            appConfig.http.corsAllowedOrigins.forEach { origin ->
                allowHost(origin.removePrefix("https://").removePrefix("http://"), schemes = listOf("https", "http"))
            }
        }
    }

    install(StatusPages) {
        exception<DomainException.DuplicateGameEvaluation> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.Conflict,
                title = "Conflict",
                detail = cause.message ?: "An evaluation for this game already exists.",
            )
        }
        exception<AuthUserAlreadyExistsException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.Conflict,
                title = "Conflict",
                detail = cause.message ?: "The auth user already exists.",
            )
        }
        exception<LastEnabledAdminRemovalException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.Conflict,
                title = "Conflict",
                detail = cause.message ?: "The last enabled admin cannot be removed.",
            )
        }
        exception<AuthUserNotFoundException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.NotFound,
                title = "Not Found",
                detail = cause.message ?: "The auth user was not found.",
            )
        }
        exception<AuthenticationThrottledException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.TooManyRequests,
                title = "Too Many Requests",
                detail = cause.message ?: "Authentication is temporarily throttled.",
                retryAfterSeconds = cause.retryAfterSeconds,
            )
        }
        exception<DomainException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.BadRequest,
                title = "Domain Validation Failed",
                detail = cause.message ?: "The request violates domain rules.",
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.BadRequest,
                title = "Invalid Request Body",
                detail = cause.message ?: "The request body could not be parsed.",
            )
        }
        exception<ContentTransformationException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.BadRequest,
                title = "Invalid Request Body",
                detail = cause.message ?: "The request body could not be parsed.",
            )
        }
        exception<SerializationException> { call, cause ->
            call.respondProblem(
                status = HttpStatusCode.BadRequest,
                title = "Invalid Request Body",
                detail = cause.message ?: "The request body contains invalid or missing fields.",
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
    retryAfterSeconds: Long? = null,
) {
    response.header(HttpHeaders.ContentType, ContentType.Application.ProblemJson.toString())
    if (retryAfterSeconds != null) {
        response.header(HttpHeaders.RetryAfter, retryAfterSeconds.toString())
    }
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
