package de.exhumedo.kmp.handball_support

import de.exhumedo.kmp.handball_support.auth.AuthRole
import de.exhumedo.kmp.handball_support.auth.AuthUserStore
import de.exhumedo.kmp.handball_support.auth.BootstrapAdmin
import de.exhumedo.kmp.handball_support.auth.JwtConfig
import de.exhumedo.kmp.handball_support.config.AppConfig
import de.exhumedo.kmp.handball_support.config.HttpConfig
import de.exhumedo.kmp.handball_support.config.StorageConfig
import de.exhumedo.kmp.handball_support.persistence.auth.JsonFileAuthUserStore
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.ApplicationTestBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Clock
import kotlin.time.Instant

internal fun fixedClock(isoInstant: String): Clock = object : Clock {
    override fun now(): Instant = Instant.parse(isoInstant)
}

internal fun createTestAppConfig(
    authUsersFile: Path = Files.createTempFile("auth-users", ".json"),
    evaluationsFile: Path = Files.createTempFile("performance-evaluations", ".json"),
    coachingDb: Path = Files.createTempFile("coaching-evaluations", ".sqlite"),
): AppConfig {
    return AppConfig(
        storage = StorageConfig(
            performanceEvaluationsFile = evaluationsFile,
            authUsersFile = authUsersFile,
            coachingEvaluationsDb = coachingDb,
        ),
        jwt = JwtConfig(
            issuer = "test-issuer",
            audience = "test-audience",
            realm = "test-realm",
            secret = "test-secret-1234567890",
            tokenTtlSeconds = 3600,
        ),
        bootstrapAdmin = BootstrapAdmin(
            username = "admin",
            password = "AdminPass123!",
        ),
        http = HttpConfig(
            corsAllowedOrigins = emptySet(),
        ),
    )
}

internal fun createTestAuthUserStore(
    clock: Clock = fixedClock("2026-04-09T10:30:00Z"),
): AuthUserStore {
    val store = JsonFileAuthUserStore(
        storagePath = Files.createTempFile("auth-users", ".json"),
        clock = clock,
        bootstrapAdmin = BootstrapAdmin(
            username = "admin",
            password = "AdminPass123!",
        ),
    )
    store.createUser(
        username = "referee",
        password = "RefereePass123!",
        role = AuthRole.REFEREE,
    )
    store.createUser(
        username = "viewer",
        password = "ViewerPass123!",
        role = AuthRole.VIEWER,
    )
    store.createUser(
        username = "referee-coach",
        password = "RefereeCoachPass123!",
        role = AuthRole.REFEREE_COACH,
    )
    store.createUser(
        username = "referee-coach-admin",
        password = "RefereeCoachAdminPass123!",
        role = AuthRole.REFEREE_COACH_ADMIN,
    )

    return store
}

internal suspend fun ApplicationTestBuilder.issueToken(
    username: String,
    password: String,
): String {
    val response = client.post("/api/auth/token") {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody("""{"username":"$username","password":"$password"}""")
    }
    val body = response.bodyAsText()
    return """"accessToken"\s*:\s*"([^"]+)"""".toRegex()
        .find(body)
        ?.groupValues
        ?.get(1)
        ?: error("No accessToken found in response body: $body")
}
