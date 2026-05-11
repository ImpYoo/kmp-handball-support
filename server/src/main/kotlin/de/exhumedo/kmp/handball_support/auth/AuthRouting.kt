package de.exhumedo.kmp.handball_support.auth

import de.exhumedo.kmp.handball_support.application.AuthUserApplicationService
import de.exhumedo.kmp.handball_support.application.AuthenticationApplicationService
import de.exhumedo.kmp.handball_support.api.respondProblem
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.authorize
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Configures token issuance and auth-user administration routes.
 *
 * @param authenticationService Application service used for token authentication.
 * @param authUserService Application service used for auth-user CRUD operations.
 * @param tokenService JWT token service.
 */
fun Application.configureAuthRouting(
    authenticationService: AuthenticationApplicationService,
    authUserService: AuthUserApplicationService,
    tokenService: JwtTokenService,
    authUserStore: AuthUserStore,
) {
    routing {
        route("/api/auth") {

            // ── Login ──────────────────────────────────────────────────────────
            // POST /api/auth/token  →  exchange credentials for a JWT
            post("/token") {
                val request = call.receive<TokenRequestDto>()
                val user = authenticationService.authenticate(request.username, request.password)
                if (user == null) {
                    call.respondProblem(
                        status = HttpStatusCode.Unauthorized,
                        title = "Unauthorized",
                        detail = "Invalid username or password.",
                    )
                    return@post
                }
                val token = tokenService.issueToken(user)
                call.respond(
                    HttpStatusCode.OK,
                    TokenResponseDto(
                        accessToken = token.token,
                        tokenType = "Bearer",
                        expiresAt = token.expiresAt,
                        role = token.role,
                    ),
                )
            }

            // ── Logout ─────────────────────────────────────────────────────────
            // POST /api/auth/logout  →  invalidate all active tokens for the caller
            post("/logout") {
                val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) ?: return@post
                authUserService.revokeTokens(actorUsername = actor.subject, username = actor.subject)
                call.respond(HttpStatusCode.NoContent)
            }

            // ── Self-service ───────────────────────────────────────────────────
            route("/users/me") {

                // GET /api/auth/users/me  →  own profile
                get {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) ?: return@get
                    val user = authUserService.findByUsername(actor.subject)
                        ?: throw AuthUserNotFoundException(actor.subject)
                    call.respond(HttpStatusCode.OK, user.toResponseDto())
                }

                // POST /api/auth/users/me/change-password  →  change own password (requires current password)
                post("/change-password") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN, AuthRole.REFEREE, AuthRole.VIEWER) ?: return@post
                    val request = call.receive<ChangePasswordRequestDto>()
                    val updated = authUserService.changePassword(
                        actorUsername = actor.subject,
                        currentPassword = request.currentPassword,
                        newPassword = request.newPassword,
                    )
                    call.respond(HttpStatusCode.OK, updated.toResponseDto())
                }
            }

            // ── Admin: user management ─────────────────────────────────────────
            route("/users") {
                get {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                    call.respond(HttpStatusCode.OK, authUserService.findAll().map { it.toResponseDto() })
                }

                post {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN) ?: return@post

                    val request = call.receive<CreateAuthUserRequestDto>()
                    val created = authUserService.createUser(
                        actorUsername = actor.subject,
                        username = request.username,
                        password = request.password,
                        role = request.role.toDomain(),
                        enabled = request.enabled,
                    )
                    call.respond(HttpStatusCode.Created, created.toResponseDto())
                }

                get("/{username}") {
                    if (call.authorize(tokenService, authUserStore, AuthRole.ADMIN) == null) return@get
                    val username = call.parameters["username"]
                        ?: return@get call.respondProblem(
                            status = HttpStatusCode.BadRequest,
                            title = "Invalid Request",
                            detail = "Path parameter 'username' is required.",
                        )

                    val user = authUserService.findByUsername(username)
                        ?: throw AuthUserNotFoundException(username)
                    call.respond(HttpStatusCode.OK, user.toResponseDto())
                }

                put("/{username}") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN) ?: return@put

                    val username = call.parameters["username"]
                        ?: return@put call.respondProblem(
                            status = HttpStatusCode.BadRequest,
                            title = "Invalid Request",
                            detail = "Path parameter 'username' is required.",
                        )

                    val request = call.receive<UpdateAuthUserRequestDto>()
                    val updated = authUserService.updateUser(
                        actorUsername = actor.subject,
                        username = username,
                        password = request.password,
                        role = request.role?.toDomain(),
                        enabled = request.enabled,
                    )
                    call.respond(HttpStatusCode.OK, updated.toResponseDto())
                }

                delete("/{username}") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN) ?: return@delete

                    val username = call.parameters["username"]
                        ?: return@delete call.respondProblem(
                            status = HttpStatusCode.BadRequest,
                            title = "Invalid Request",
                            detail = "Path parameter 'username' is required.",
                        )

                    authUserService.deleteUser(
                        actorUsername = actor.subject,
                        username = username,
                    )
                    call.respond(HttpStatusCode.NoContent)
                }

                post("/{username}/revoke-tokens") {
                    val actor = call.authorize(tokenService, authUserStore, AuthRole.ADMIN) ?: return@post
                    val username = call.parameters["username"]
                        ?: return@post call.respondProblem(
                            status = HttpStatusCode.BadRequest,
                            title = "Invalid Request",
                            detail = "Path parameter 'username' is required.",
                        )

                    val revoked = authUserService.revokeTokens(
                        actorUsername = actor.subject,
                        username = username,
                    )
                    call.respond(HttpStatusCode.OK, revoked.toResponseDto())
                }
            }
        }
    }
}
