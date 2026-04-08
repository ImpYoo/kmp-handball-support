package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.application.AuthUserApplicationService
import de.exhumedo.kmp.handball_support.application.AuthenticationApplicationService
import de.exhumedo.kmp.handball_support.application.PerformanceEvaluationApplicationService
import de.exhumedo.kmp.handball_support.application.UuidEvaluationIdGenerator
import de.exhumedo.kmp.handball_support.api.configureHttp
import de.exhumedo.kmp.handball_support.api.configureRouting
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.auth.configureAuthRouting
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.config.AppConfigLoader
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.persistence.JsonFilePerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.persistence.auth.JsonFileAuthUserStore
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.LoginAttemptGuard
import de.exhumedo.kmp.handball_support.security.Pbkdf2PasswordHasher
import de.exhumedo.kmp.handball_support.security.Slf4jAuthAuditLogger
import de.exhumedo.kmp.handball_support.security.configureSecurity
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.time.Clock

/**
 * Server entry point.
 */
fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

/**
 * Configures the Ktor application with the file-backed repository and API routes.
 *
 * @param appConfig Resolved server configuration.
 * @param repository Repository adapter used by the API.
 * @param authUserStore Auth user store used by auth services.
 * @param clock Clock used to derive creation timestamps for new evaluations.
 * @param jwtClock Clock used for issuing JWT access tokens.
 */
fun Application.module(
    appConfig: AppConfig = AppConfigLoader.load(),
    clock: Clock = Clock.System,
    jwtClock: Clock = Clock.System,
    repository: PerformanceEvaluationRepository = JsonFilePerformanceEvaluationRepository(
        appConfig.storage.performanceEvaluationsFile,
    ),
    authUserStore: AuthUserStore = defaultAuthUserStore(appConfig, clock),
) {
    val tokenService = JwtTokenService(
        config = appConfig.jwt,
        clock = jwtClock,
    )
    val applicationService = PerformanceEvaluationApplicationService(
        repository = repository,
        idGenerator = UuidEvaluationIdGenerator(),
        clock = clock,
    )
    val auditLogger = Slf4jAuthAuditLogger()
    val authenticationApplicationService = AuthenticationApplicationService(
        userStore = authUserStore,
        attemptGuard = LoginAttemptGuard(clock = jwtClock),
        auditLogger = auditLogger,
    )
    val authUserApplicationService = AuthUserApplicationService(
        userStore = authUserStore,
        auditLogger = auditLogger,
        clock = jwtClock,
    )

    configureHttp(appConfig)
    configureSecurity(appConfig.jwt, tokenService)
    configureAuthRouting(authenticationApplicationService, authUserApplicationService, tokenService, authUserStore)
    configureRouting(repository, applicationService, tokenService, authUserStore)
}

private fun defaultAuthUserStore(
    appConfig: AppConfig,
    clock: Clock,
): AuthUserStore {
    return JsonFileAuthUserStore(
        storagePath = appConfig.storage.authUsersFile,
        passwordHasher = Pbkdf2PasswordHasher(),
        clock = clock,
        bootstrapAdmin = appConfig.bootstrapAdmin,
    )
}
