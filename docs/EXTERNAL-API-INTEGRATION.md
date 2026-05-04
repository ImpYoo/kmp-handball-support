# External API Integration Guide

## Overview

The `ExternalApiGateway` provides a clean outbound HTTP client abstraction for calling external REST APIs. It isolates transport concerns from business logic using a hexagonal port–adapter pattern consistent with your existing repository pattern.

## Architecture

```
┌─ API Layer (routes)
│
├─ Application Layer (services)
│  └─ ExternalApiGateway (port interface)
│
└─ Integration Layer (adapters)
   └─ KtorExternalApiGateway (Ktor HTTP implementation)
```

## Key Components

### 1. Port: `ExternalApiGateway`
**Location**: `application/ExternalApiGateway.kt`

Defines three example GET methods:

```kotlin
interface ExternalApiGateway {
    suspend fun getStatus(): ExternalStatusDto
    suspend fun getTeam(teamId: String): ExternalTeamDto
    suspend fun getFixtures(season: String, limit: Int): List<ExternalFixtureDto>
}
```

**Throws**: `ExternalApiException` on any transport/HTTP error.

### 2. Adapter: `KtorExternalApiGateway`
**Location**: `integration/KtorExternalApiGateway.kt`

Implements the port using Ktor's `HttpClient`:
- Handles JSON serialization/deserialization
- Maps transport exceptions to `ExternalApiException`
- Injects optional API key header via config

### 3. Configuration: `ExternalApiConfig`
**Location**: `config/AppConfig.kt`

Controls the external API integration:

```kotlin
data class ExternalApiConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val apiKey: String? = null,
    val apiKeyHeaderName: String = "X-API-Key",
    val requestTimeoutMillis: Long = 5_000,
    val connectTimeoutMillis: Long = 3_000,
)
```

### 4. Wiring: `Application.module()`
**Location**: `Application.kt`

Constructs and injects `ExternalApiGateway` via `defaultExternalApiGateway()`:
- Returns `DisabledExternalApiGateway` if not enabled (safe for dev/test)
- Creates `HttpClient(CIO)` with JSON and timeouts
- Registers shutdown hook to close client

## Environment Setup

### Enable via Environment Variables
```bash
export EXTERNAL_API_ENABLED=true
export EXTERNAL_API_BASE_URL=https://api.example.com
export EXTERNAL_API_KEY=your-api-key
export EXTERNAL_API_REQUEST_TIMEOUT_MS=10000
export EXTERNAL_API_CONNECT_TIMEOUT_MS=5000
./gradlew :server:run
```

### Enable via .env File
```
EXTERNAL_API_ENABLED=true
EXTERNAL_API_BASE_URL=https://api.example.com
EXTERNAL_API_KEY=your-api-key
EXTERNAL_API_KEY_HEADER=Authorization
```

### System Properties
```bash
./gradlew :server:run \
  -Dexternal.api.enabled=true \
  -Dexternal.api.base-url=https://api.example.com \
  -Dexternal.api.key=your-api-key
```

## Usage Example

### In an Application Service
```kotlin
class MyApplicationService(
    private val externalGateway: ExternalApiGateway
) {
    suspend fun enrichWithExternalData(teamId: String): MyResult {
        return try {
            val team = externalGateway.getTeam(teamId)
            val fixtures = externalGateway.getFixtures("2025", limit = 5)
            MyResult.Success(team, fixtures)
        } catch (e: ExternalApiException) {
            logger.error("Failed to fetch external data: ${e.message}")
            MyResult.Failure(e)
        }
    }
}
```

### In an API Route
```kotlin
route("/api/enriched") {
    get("/{teamId}") {
        val teamId = call.parameters["teamId"] ?: return@get call.respondProblem(...)
        try {
            val status = externalGateway.getStatus()
            call.respond(HttpStatusCode.OK, status)
        } catch (e: ExternalApiException) {
            call.respondProblem(
                status = HttpStatusCode.BadGateway,
                title = "External API Unavailable",
                detail = e.message ?: "Could not reach external service."
            )
        }
    }
}
```

## Testing

### Using MockEngine
```kotlin
val mockEngine = MockEngine { request ->
    respond(
        content = """{"status":"operational","version":"1.0.0"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType to listOf("application/json")),
    )
}

val httpClient = HttpClient(mockEngine) {
    install(ContentNegotiation) { json() }
}

val gateway = KtorExternalApiGateway(
    httpClient = httpClient,
    config = ExternalApiConfig(enabled = true, baseUrl = "https://mock.local")
)

val status = runBlocking { gateway.getStatus() }
assertEquals("operational", status.status)
```

### Disabled Gateway in Tests
By default, test config has `externalApi.enabled = false`:

```kotlin
createTestAppConfig()  // Uses DisabledExternalApiGateway
```

No external calls are made unless explicitly wired.

## Test Files

| File | Tests | Purpose |
|------|-------|---------|
| `ExternalApiGatewayTest.kt` | 5 | Ktor adapter behavior with MockEngine |
| `ExternalApiConfigTest.kt` | 6 | Config validation, env resolution |
| `ExternalApiIntegrationTest.kt` | 2 | Module wiring, disabled behavior |

Run tests:
```bash
./gradlew :server:test --tests "ExternalApi*"
```

## Error Handling

All gateway methods throw `ExternalApiException` for:
- Network timeouts
- HTTP errors (4xx, 5xx)
- Serialization failures
- Connection errors

Example flow:
```kotlin
try {
    val team = gateway.getTeam("T1")
} catch (e: ExternalApiException) {
    // Handle gracefully: retry, fallback, or propagate to user
    logger.error("External API call failed", e)
    throw DomainException("Could not fetch team data")
}
```

## Extending the Gateway

### Adding a New GET Method
1. **Define** in `ExternalApiGateway` port (public DTO, suspend function)
2. **Implement** in `KtorExternalApiGateway` (internal response DTO, mapping)
3. **Wire** response DTO to public DTO
4. **Test** with MockEngine scenario

Example:
```kotlin
// In ExternalApiGateway
suspend fun getLeague(leagueId: String): ExternalLeagueDto

// In KtorExternalApiGateway
override suspend fun getLeague(leagueId: String): ExternalLeagueDto {
    val response = execute("getLeague") {
        httpClient.get("${config.baseUrl}/leagues/$leagueId") {
            applyAuth(this)
        }.body<LeagueResponseDto>()
    }
    return ExternalLeagueDto(...)
}

// In test
val league = runBlocking { gateway.getLeague("L1") }
```

### Using a Different HTTP Method
For `POST`, `PUT`, `DELETE`, extend the adapter with additional methods and implement the same error handling pattern.

## Troubleshooting

### Gateway returns disabled error
```
ExternalApiException: External API integration is disabled. Cannot invoke 'getStatus'.
```
**Solution**: Set `EXTERNAL_API_ENABLED=true` and configure base URL.

### Connection timeout
```
ExternalApiException: External API timeout while calling 'getTeam'.
```
**Solution**: Increase `requestTimeoutMillis` or `connectTimeoutMillis` in config.

### 401/403 on requests
```
ExternalApiException: External API responded with 401 during 'getTeam'.
```
**Solution**: Verify `EXTERNAL_API_KEY` and `EXTERNAL_API_KEY_HEADER` are correct.

## Files Modified

- ✓ `gradle/libs.versions.toml` — Ktor client libraries
- ✓ `server/build.gradle.kts` — dependencies
- ✓ `server/src/main/kotlin/.../application/ExternalApiGateway.kt` — port interface (new)
- ✓ `server/src/main/kotlin/.../integration/KtorExternalApiGateway.kt` — adapter (new)
- ✓ `server/src/main/kotlin/.../config/AppConfig.kt` — extended with `ExternalApiConfig`
- ✓ `server/src/main/kotlin/.../Application.kt` — wiring + factory
- ✓ `server/src/test/.../ExternalApiGatewayTest.kt` — adapter tests (new)
- ✓ `server/src/test/.../ExternalApiConfigTest.kt` — config tests (new)

## Next Steps

1. **Inject** `ExternalApiGateway` into your application services
2. **Define** domain-specific methods for your use cases (extend the three examples)
3. **Add** retry logic / circuit breaker for resilience
4. **Monitor** outbound call latencies and errors
5. **Document** external API contract in `docs/openapi.yaml`
