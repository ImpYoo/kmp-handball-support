package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.application.AuthUserApplicationService
import de.exhumedo.kmp.handball_support.application.AuthenticationApplicationService
import de.exhumedo.kmp.handball_support.application.MatchApplicationService
import de.exhumedo.kmp.handball_support.application.PerformanceEvaluationApplicationService
import de.exhumedo.kmp.handball_support.application.UuidEvaluationIdGenerator
import de.exhumedo.kmp.handball_support.api.configureCoachingRouting
import de.exhumedo.kmp.handball_support.api.configureHttp
import de.exhumedo.kmp.handball_support.api.configurePhaseRouting
import de.exhumedo.kmp.handball_support.api.configureRouting
import de.exhumedo.kmp.handball_support.api.configureSportradarRouting
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.auth.configureAuthRouting
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.config.AppConfigLoader
import de.exhumedo.kmp.handball_support.domain.rating.repository.PerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.persistence.JsonFilePerformanceEvaluationRepository
import de.exhumedo.kmp.handball_support.persistence.MockPhaseRepository
import de.exhumedo.kmp.handball_support.persistence.PerformanceEvaluationBasedVoteRepository
import de.exhumedo.kmp.handball_support.persistence.SportradarPhaseRepositoryAdapter
import de.exhumedo.kmp.handball_support.persistence.auth.JsonFileAuthUserStore
import de.exhumedo.kmp.handball_support.persistence.coaching.SqliteCoachingEvaluationRepository
import de.exhumedo.kmp.handball_support.security.JwtTokenService
import de.exhumedo.kmp.handball_support.security.LoginAttemptGuard
import de.exhumedo.kmp.handball_support.security.Pbkdf2PasswordHasher
import de.exhumedo.kmp.handball_support.security.Slf4jAuthAuditLogger
import de.exhumedo.kmp.handball_support.security.configureSecurity
import de.exhumedo.kmp.handball_support.sportradar.Slf4jSportradarTelemetry
import de.exhumedo.kmp.handball_support.sportradar.cache.FixturesCache
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarClientOptions
import de.exhumedo.kmp.handball_support.sportradar.client.SportradarHttpClientFactory
import de.exhumedo.kmp.handball_support.sportradar.config.TournamentConfigLoader
import de.exhumedo.kmp.handball_support.sportradar.repository.SportradarPhaseRepository
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.time.Clock
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("de.exhumedo.kmp.handball_support.Application")

/**
 * Server entry point.
 */
fun main() {
    val appConfig = AppConfigLoader.load()
    embeddedServer(Netty, port = appConfig.serverPort, host = "0.0.0.0") {
        module(appConfig = appConfig)
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
    val sportradarRepo = buildSportradarPhaseRepository(appConfig)
    val matchApplicationService = MatchApplicationService(
        phaseRepository = if (sportradarRepo != null)
            SportradarPhaseRepositoryAdapter(sportradarRepo)
        else
            MockPhaseRepository(),
        voteRepository = PerformanceEvaluationBasedVoteRepository(repository),
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

    val coachingRepository = SqliteCoachingEvaluationRepository(appConfig.storage.coachingEvaluationsDb)
    val coachingApplicationService = de.exhumedo.kmp.handball_support.application.coaching.CoachingApplicationService(
        repository = coachingRepository,
        clock = clock,
    )

    configureHttp(appConfig)
    configureSecurity(appConfig.jwt, tokenService)
    configureAuthRouting(authenticationApplicationService, authUserApplicationService, tokenService, authUserStore)
    configureRouting(repository, applicationService, tokenService, authUserStore)
    configurePhaseRouting(matchApplicationService, tokenService, authUserStore)
    configureSportradarRouting(sportradarRepo, tokenService, authUserStore)
    configureCoachingRouting(coachingApplicationService, tokenService, authUserStore)
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

/**
 * Builds the raw [SportradarPhaseRepository] when Sportradar integration is enabled and
 * properly configured, or returns `null` so callers can fall back to [MockPhaseRepository].
 */
private fun buildSportradarPhaseRepository(appConfig: AppConfig): SportradarPhaseRepository? {
    val ext = appConfig.externalApi
    if (!ext.enabled) {
        logger.warn(
            "EXTERNAL_API_ENABLED=false — MockPhaseRepository is active. " +
                "Set EXTERNAL_API_ENABLED=true and provide tournaments.json to use real Sportradar data.",
        )
        return null
    }

    val tournamentsPath = Paths.get(ext.tournamentsFile)
    if (!Files.exists(tournamentsPath)) {
        logger.error(
            "EXTERNAL_API_ENABLED=true but tournaments file not found at '{}'. " +
                "Falling back to MockPhaseRepository.",
            tournamentsPath,
        )
        return null
    }

    val configs = TournamentConfigLoader.fromJson(Files.readString(tournamentsPath))
    if (configs.isEmpty()) {
        logger.error(
            "tournaments.json at '{}' is empty. Falling back to MockPhaseRepository.",
            tournamentsPath,
        )
        return null
    }

    logger.info("Sportradar integration ENABLED — loading {} tournament(s) from '{}'", configs.size, tournamentsPath)

    val clientOptions = SportradarClientOptions(
        hblBaseUrl             = ext.baseUrl,
        accessLevel            = ext.accessLevel,
        language               = ext.language,
        timeZone               = ext.timeZone,
        product                = ext.product,
        apiKey                 = ext.apiKey,
        apiKeyHeaderName       = ext.apiKeyHeaderName,
        sendApiKeyAsQueryParam = ext.sendApiKeyAsQueryParam,
        apiKeyQueryParamName   = ext.apiKeyQueryParamName,
    )

    return SportradarPhaseRepository(
        httpClient    = SportradarHttpClientFactory.createJvmDefault(
            connectTimeoutMillis = ext.connectTimeoutMillis,
            requestTimeoutMillis = ext.requestTimeoutMillis,
        ),
        configs       = configs,
        cache         = FixturesCache(),
        clientOptions = clientOptions,
        telemetry     = Slf4jSportradarTelemetry(),
    )
}

