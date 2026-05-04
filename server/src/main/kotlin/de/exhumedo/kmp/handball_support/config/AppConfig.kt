package de.exhumedo.kmp.handball_support.config

import de.exhumedo.kmp.handball_support.auth.BootstrapAdmin
import de.exhumedo.kmp.handball_support.auth.JwtConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Top-level server configuration resolved from environment variables, system properties,
 * and optionally a local `.env` file.
 *
 * @property storage Storage file locations.
 * @property jwt JWT signing and verification configuration.
 * @property bootstrapAdmin Bootstrap admin configuration used for first startup.
 * @property http HTTP-facing configuration such as allowed CORS origins.
 * @property externalApi Outbound external API integration configuration.
 */
data class AppConfig(
    val storage: StorageConfig,
    val jwt: JwtConfig,
    val bootstrapAdmin: BootstrapAdmin?,
    val http: HttpConfig,
    val externalApi: ExternalApiConfig = ExternalApiConfig(),
)

/**
 * File-system locations used by the server.
 *
 * @property performanceEvaluationsFile JSON file storing evaluations.
 * @property authUsersFile JSON file storing auth users.
 */
data class StorageConfig(
    val performanceEvaluationsFile: Path,
    val authUsersFile: Path,
)

/**
 * HTTP-facing server configuration.
 *
 * @property corsAllowedOrigins Explicit browser origins allowed for cross-origin API requests.
 */
data class HttpConfig(
    val corsAllowedOrigins: Set<String>,
)

/**
 * Outbound integration configuration for a remote HTTP API (Sportradar FMP feed).
 *
 * @property enabled Whether the outbound integration is active.
 * @property baseUrl Base URL of the remote API.
 * @property language Feed language segment (e.g. "en").
 * @property timeZone Feed time-zone segment (e.g. "Europe:Berlin").
 * @property product Feed product segment (e.g. "gismo").
 * @property apiKey Optional API key sent as a request header or query parameter.
 * @property apiKeyHeaderName Header name used to transmit the API key.
 * @property sendApiKeyAsQueryParam When true the key is appended as a query parameter instead of a header.
 * @property apiKeyQueryParamName Query parameter name when [sendApiKeyAsQueryParam] is true.
 * @property requestTimeoutMillis Per-request timeout in milliseconds.
 * @property connectTimeoutMillis Connection timeout in milliseconds.
 */
data class ExternalApiConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "https://hbl.fmp.sportradar.com",
    val language: String = "en",
    val timeZone: String = "Europe:Berlin",
    val product: String = "gismo",
    val apiKey: String? = null,
    val apiKeyHeaderName: String = "X-API-Key",
    val sendApiKeyAsQueryParam: Boolean = false,
    val apiKeyQueryParamName: String = "api_key",
    val requestTimeoutMillis: Long = 5_000L,
    val connectTimeoutMillis: Long = 3_000L,
)

object AppConfigLoader {
    /**
     * Resolves the effective application configuration.
     *
     * @param dotEnvPath Optional `.env` file path. Defaults to the project-root `.env`.
     * @return Resolved application configuration.
     */
    fun load(dotEnvPath: Path = Paths.get(".env")): AppConfig {
        val dotEnv = loadDotEnv(dotEnvPath)
        val developmentMode = resolveBoolean(
            envKey = "APP_DEVELOPMENT",
            systemPropertyKey = "app.development",
            dotEnv = dotEnv,
        ) ?: resolveBoolean(
            envKey = "IO_KTOR_DEVELOPMENT",
            systemPropertyKey = "io.ktor.development",
            dotEnv = dotEnv,
        ) ?: false

        val jwtSecret = resolveConfigValue(
            envKey = "JWT_SECRET",
            systemPropertyKey = "jwt.secret",
            dotEnv = dotEnv,
        ) ?: if (developmentMode) {
            "development-secret-change-me"
        } else {
            throw IllegalStateException(
                "JWT secret is required outside development mode. Configure JWT_SECRET or -Djwt.secret.",
            )
        }

        val config = AppConfig(
            storage = StorageConfig(
                performanceEvaluationsFile = Paths.get(
                    resolveConfigValue(
                        envKey = "PERFORMANCE_EVALUATIONS_FILE",
                        systemPropertyKey = "performance.evaluations.file",
                        dotEnv = dotEnv,
                    ) ?: "server/data/performance-evaluations.json",
                ),
                authUsersFile = Paths.get(
                    resolveConfigValue(
                        envKey = "AUTH_USERS_FILE",
                        systemPropertyKey = "auth.users.file",
                        dotEnv = dotEnv,
                    ) ?: "server/data/auth-users.json",
                ),
            ),
            jwt = JwtConfig(
                issuer = resolveConfigValue(
                    envKey = "JWT_ISSUER",
                    systemPropertyKey = "jwt.issuer",
                    dotEnv = dotEnv,
                ) ?: "de.exhumedo.kmp.handball_support",
                audience = resolveConfigValue(
                    envKey = "JWT_AUDIENCE",
                    systemPropertyKey = "jwt.audience",
                    dotEnv = dotEnv,
                ) ?: "handball-support-api",
                realm = resolveConfigValue(
                    envKey = "JWT_REALM",
                    systemPropertyKey = "jwt.realm",
                    dotEnv = dotEnv,
                ) ?: "handball-support",
                secret = jwtSecret,
                tokenTtlSeconds = (
                    resolveConfigValue(
                        envKey = "JWT_TTL_SECONDS",
                        systemPropertyKey = "jwt.ttl.seconds",
                        dotEnv = dotEnv,
                    ) ?: "3600"
                    ).toLong(),
            ),
            bootstrapAdmin = resolveBootstrapAdmin(dotEnv),
            http = HttpConfig(
                corsAllowedOrigins = resolveCsvConfig(
                    envKey = "CORS_ALLOWED_ORIGINS",
                    systemPropertyKey = "cors.allowed.origins",
                    dotEnv = dotEnv,
                ),
            ),
            externalApi = ExternalApiConfig(
                enabled = resolveBoolean(
                    envKey = "EXTERNAL_API_ENABLED",
                    systemPropertyKey = "external.api.enabled",
                    dotEnv = dotEnv,
                ) ?: false,
                baseUrl = resolveConfigValue(
                    envKey = "EXTERNAL_API_BASE_URL",
                    systemPropertyKey = "external.api.base-url",
                    dotEnv = dotEnv,
                ) ?: "https://hbl.fmp.sportradar.com",
                language = resolveConfigValue(
                    envKey = "EXTERNAL_API_LANGUAGE",
                    systemPropertyKey = "external.api.language",
                    dotEnv = dotEnv,
                ) ?: "en",
                timeZone = resolveConfigValue(
                    envKey = "EXTERNAL_API_TIMEZONE",
                    systemPropertyKey = "external.api.timezone",
                    dotEnv = dotEnv,
                ) ?: "Europe:Berlin",
                product = resolveConfigValue(
                    envKey = "EXTERNAL_API_PRODUCT",
                    systemPropertyKey = "external.api.product",
                    dotEnv = dotEnv,
                ) ?: "gismo",
                apiKey = resolveConfigValue(
                    envKey = "EXTERNAL_API_KEY",
                    systemPropertyKey = "external.api.key",
                    dotEnv = dotEnv,
                ),
                apiKeyHeaderName = resolveConfigValue(
                    envKey = "EXTERNAL_API_KEY_HEADER",
                    systemPropertyKey = "external.api.key-header",
                    dotEnv = dotEnv,
                ) ?: "X-API-Key",
                sendApiKeyAsQueryParam = resolveBoolean(
                    envKey = "EXTERNAL_API_KEY_AS_QUERY_PARAM",
                    systemPropertyKey = "external.api.key-as-query-param",
                    dotEnv = dotEnv,
                ) ?: false,
                apiKeyQueryParamName = resolveConfigValue(
                    envKey = "EXTERNAL_API_KEY_QUERY_PARAM_NAME",
                    systemPropertyKey = "external.api.key-query-param-name",
                    dotEnv = dotEnv,
                ) ?: "api_key",
                requestTimeoutMillis = resolveConfigValue(
                    envKey = "EXTERNAL_API_REQUEST_TIMEOUT_MS",
                    systemPropertyKey = "external.api.request-timeout-ms",
                    dotEnv = dotEnv,
                )?.toLongOrNull() ?: 5_000L,
                connectTimeoutMillis = resolveConfigValue(
                    envKey = "EXTERNAL_API_CONNECT_TIMEOUT_MS",
                    systemPropertyKey = "external.api.connect-timeout-ms",
                    dotEnv = dotEnv,
                )?.toLongOrNull() ?: 3_000L,
            ),
        )

        validate(config)
        return config
    }

    private fun resolveBootstrapAdmin(dotEnv: Map<String, String>): BootstrapAdmin? {
        val username = resolveConfigValue(
            envKey = "AUTH_BOOTSTRAP_ADMIN_USERNAME",
            systemPropertyKey = "auth.bootstrap.admin.username",
            dotEnv = dotEnv,
        ) ?: return null

        val password = resolveConfigValue(
            envKey = "AUTH_BOOTSTRAP_ADMIN_PASSWORD",
            systemPropertyKey = "auth.bootstrap.admin.password",
            dotEnv = dotEnv,
        ) ?: return null

        return BootstrapAdmin(
            username = username,
            password = password,
        )
    }

    private fun resolveConfigValue(
        envKey: String,
        systemPropertyKey: String,
        dotEnv: Map<String, String>,
    ): String? {
        return System.getenv(envKey)
            ?: System.getProperty(systemPropertyKey)
            ?: dotEnv[envKey]
    }

    private fun resolveBoolean(
        envKey: String,
        systemPropertyKey: String,
        dotEnv: Map<String, String>,
    ): Boolean? {
        return resolveConfigValue(envKey, systemPropertyKey, dotEnv)?.toBooleanStrictOrNull()
    }

    private fun resolveCsvConfig(
        envKey: String,
        systemPropertyKey: String,
        dotEnv: Map<String, String>,
    ): Set<String> {
        return resolveConfigValue(envKey, systemPropertyKey, dotEnv)
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.toSet()
            ?: emptySet()
    }

    private fun loadDotEnv(path: Path): Map<String, String> {
        if (!Files.exists(path)) {
            return emptyMap()
        }

        return Files.readAllLines(path)
            .mapNotNull(::parseDotEnvLine)
            .toMap()
    }

    private fun parseDotEnvLine(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }

        val separatorIndex = trimmed.indexOf('=')
        if (separatorIndex <= 0) {
            return null
        }

        val key = trimmed.substring(0, separatorIndex).trim()
        if (key.isBlank()) {
            return null
        }

        val value = trimmed.substring(separatorIndex + 1)
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        return key to value
    }

    private fun validate(config: AppConfig) {
        require(config.jwt.issuer.isNotBlank()) { "JWT issuer must not be blank." }
        require(config.jwt.audience.isNotBlank()) { "JWT audience must not be blank." }
        require(config.jwt.realm.isNotBlank()) { "JWT realm must not be blank." }
        require(config.jwt.secret.isNotBlank()) { "JWT secret must not be blank." }
        require(config.jwt.tokenTtlSeconds > 0) { "JWT token TTL must be greater than zero." }
        require(config.storage.authUsersFile != config.storage.performanceEvaluationsFile) {
            "AUTH_USERS_FILE and PERFORMANCE_EVALUATIONS_FILE must not point to the same file."
        }
        require(config.http.corsAllowedOrigins.none(String::isBlank)) {
            "CORS_ALLOWED_ORIGINS must not contain blank origins."
        }
        require(config.externalApi.apiKeyHeaderName.isNotBlank()) {
            "EXTERNAL_API_KEY_HEADER must not be blank."
        }
        require(config.externalApi.requestTimeoutMillis > 0) {
            "EXTERNAL_API_REQUEST_TIMEOUT_MS must be greater than zero."
        }
        require(config.externalApi.connectTimeoutMillis > 0) {
            "EXTERNAL_API_CONNECT_TIMEOUT_MS must be greater than zero."
        }
        if (config.externalApi.enabled) {
            require(config.externalApi.baseUrl.isNotBlank()) {
                "EXTERNAL_API_BASE_URL is required when EXTERNAL_API_ENABLED=true."
            }
            require(
                config.externalApi.baseUrl.startsWith("http://") ||
                    config.externalApi.baseUrl.startsWith("https://"),
            ) {
                "EXTERNAL_API_BASE_URL must start with http:// or https://."
            }
        }

        config.bootstrapAdmin?.let { bootstrapAdmin ->
            require(bootstrapAdmin.username.trim().isNotBlank()) {
                "Bootstrap admin username must not be blank."
            }
            require(bootstrapAdmin.password.isNotBlank()) {
                "Bootstrap admin password must not be blank."
            }
        }
    }
}
