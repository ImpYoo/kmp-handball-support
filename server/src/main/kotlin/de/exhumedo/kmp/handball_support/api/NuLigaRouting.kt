package de.exhumedo.kmp.handball_support.api

import de.exhumedo.kmp.handball_support.api.dto.NuLigaMatchResolutionRequest
import de.exhumedo.kmp.handball_support.api.dto.toResponseDto
import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.nuliga.NuLigaApplicationService
import de.exhumedo.kmp.handball_support.nuliga.NuLigaError
import de.exhumedo.kmp.handball_support.nuliga.NuLigaResult
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * `POST /api/nuliga/match-resolutions` — resolve a pasted nuLiga live URL into teams,
 * rosters, officials and venue. Stateless: the server stores nothing. The endpoint reaches
 * a third party, so it requires a signed-in user; upstream failures surface as 502/504
 * problems, malformed or foreign URLs as 400.
 */
fun Application.configureNuLigaRouting(
    nuLigaService: NuLigaApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        route("/api/nuliga/match-resolutions") {
            post {
                if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) == null) return@post

                val request = try {
                    call.receive<NuLigaMatchResolutionRequest>()
                } catch (e: Exception) {
                    return@post call.respondProblem(
                        HttpStatusCode.BadRequest,
                        "Invalid Request",
                        "The body must be JSON with a url field.",
                    )
                }

                val resolution = withContext(Dispatchers.IO) {
                    try {
                        nuLigaService.resolve(request.url)
                    } catch (e: NuLigaApplicationService.InvalidMatchUrlException) {
                        NuLigaResult.Failure(NuLigaError.NetworkError(e))
                    }
                }

                when (resolution) {
                    is NuLigaResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        resolution.value.toResponseDto(
                            sourceUrl = request.url,
                            resolvedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                        ),
                    )
                    is NuLigaResult.Failure -> when (val error = resolution.error) {
                        is NuLigaError.NetworkError ->
                            if (error.cause is NuLigaApplicationService.InvalidMatchUrlException) {
                                call.respondProblem(
                                    HttpStatusCode.BadRequest,
                                    "Invalid Match URL",
                                    error.cause.message ?: "The url cannot be resolved.",
                                )
                            } else if (error.cause is HttpRequestTimeoutException) {
                                call.respondProblem(
                                    HttpStatusCode.GatewayTimeout,
                                    "Upstream Timeout",
                                    "nuLiga did not answer in time.",
                                )
                            } else {
                                call.respondProblem(
                                    HttpStatusCode.BadGateway,
                                    "Upstream Failure",
                                    "nuLiga is not reachable: ${error.cause.message ?: "network error"}",
                                )
                            }
                        is NuLigaError.HttpError -> call.respondProblem(
                            HttpStatusCode.BadGateway,
                            "Upstream Failure",
                            "nuLiga answered ${error.statusCode}. The meeting may not be published yet.",
                        )
                        is NuLigaError.MissingVersion -> call.respondProblem(
                            HttpStatusCode.BadGateway,
                            "Upstream Failure",
                            "nuLiga did not return a version for this meeting, so the rosters cannot be read consistently.",
                        )
                        is NuLigaError.ParseError -> call.respondProblem(
                            HttpStatusCode.BadGateway,
                            "Upstream Failure",
                            "nuLiga returned a payload this service could not read.",
                        )
                    }
                }
            }
        }
    }
}