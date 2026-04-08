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
 * @property developmentMode Whether development-friendly fallbacks are allowed.
 */
data class AppConfig(
    val storage: StorageConfig,
    val jwt: JwtConfig,
    val bootstrapAdmin: BootstrapAdmin?,
    val http: HttpConfig,
    val developmentMode: Boolean,
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
 * Loads application configuration from environment variables, JVM properties, and a local `.env` file.
 */
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
            developmentMode = developmentMode,
        )

        validate(config)
        return config
    }

    private fun resolveBootstrapAdmin(dotEnv: Map<String, String>): BootstrapAdmin? {
        val password = resolveConfigValue(
            envKey = "AUTH_BOOTSTRAP_ADMIN_PASSWORD",
            systemPropertyKey = "auth.bootstrap.admin.password",
            dotEnv = dotEnv,
        ) ?: return null

        val username = resolveConfigValue(
            envKey = "AUTH_BOOTSTRAP_ADMIN_USERNAME",
            systemPropertyKey = "auth.bootstrap.admin.username",
            dotEnv = dotEnv,
        ) ?: "admin"

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
