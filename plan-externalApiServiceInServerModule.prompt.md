## Plan: Outbound API Service in Server Module

Add an outbound-API integration as a clean adapter behind an application-level port, matching your current server layering (`api` -> `application` -> adapters like `persistence`). HTTP transport concerns are isolated, config is explicit via AppConfig, and wiring happens in `Application.module` so API routes call the gateway without direct client coupling.

### Implementation Complete ✓

#### 1. Port & Domain Contracts
- **File**: `server/src/main/kotlin/de/exhumedo/kmp/handball_support/application/ExternalApiGateway.kt`
- **Interface**: `ExternalApiGateway` with three example GET methods:
  - `getStatus(): ExternalStatusDto` — Fetch API health/version
  - `getTeam(teamId: String): ExternalTeamDto` — Get team by ID  
  - `getFixtures(season: String, limit: Int): List<ExternalFixtureDto>` — List fixtures with pagination
- **DTOs**: `ExternalStatusDto`, `ExternalTeamDto`, `ExternalFixtureDto` (public domain models)
- **Transport Exception**: `ExternalApiException(message, cause)` for all outbound failures
- **Disabled Default**: `DisabledExternalApiGateway` throws if integration is not enabled

#### 2. Ktor HTTP Adapter  
- **File**: `server/src/main/kotlin/de/exhumedo/kmp/handball_support/integration/KtorExternalApiGateway.kt`
- **Implementation**: `KtorExternalApiGateway(httpClient, config)` with:
  - Three GET implementations matching the port contract
  - Adapter DTOs (internal data classes with Ktor-specific `@SerialName` annotations for snake_case mapping)
  - Unified error handler: maps `HttpRequestTimeoutException`, `ResponseException`, `IOException` → `ExternalApiException`
  - Optional API key header injection via `applyAuth()`

#### 3. Configuration (Extended AppConfig)
- **File**: `server/src/main/kotlin/de/exhumedo/kmp/handball_support/config/AppConfig.kt`
- **New Data Class**: `ExternalApiConfig` with:
  - `enabled: Boolean` — toggle for outbound integration (default: false)
  - `baseUrl: String` — remote API base URL (required when enabled)
  - `apiKey: String?` — optional bearer/API key  
  - `apiKeyHeaderName: String` — header name for auth (default: "X-API-Key")
  - `requestTimeoutMillis: Long` — request timeout (default: 5000 ms)
  - `connectTimeoutMillis: Long` — connection timeout (default: 3000 ms)
- **Environment Resolution**:
  - `EXTERNAL_API_ENABLED` / `external.api.enabled`
  - `EXTERNAL_API_BASE_URL` / `external.api.base-url`
  - `EXTERNAL_API_KEY` / `external.api.key`
  - `EXTERNAL_API_KEY_HEADER` / `external.api.key-header`
  - `EXTERNAL_API_REQUEST_TIMEOUT_MS` / `external.api.request-timeout-ms`
  - `EXTERNAL_API_CONNECT_TIMEOUT_MS` / `external.api.connect-timeout-ms`
- **Validation**: Base URL scheme enforcement (http/https), timeouts > 0, header non-blank

#### 4. Dependencies
- **Added to `gradle/libs.versions.toml`**:
  - `ktor-clientCore`, `ktor-clientCio`, `ktor-clientContentNegotiation`, `ktor-clientMock`
- **Added to `server/build.gradle.kts`**:
  - Implementation: client core, CIO engine, content negotiation
  - Test: mock engine

#### 5. Module Wiring  
- **File**: `server/src/main/kotlin/de/exhumedo/kmp/handball_support/Application.kt`
- **Function**: `defaultExternalApiGateway(config, application)` — factory logic:
  - Returns `DisabledExternalApiGateway` if not enabled
  - Constructs `HttpClient(CIO)` with JSON content negotiation & timeouts
  - Registers shutdown hook to close client on app stop
  - Returns `KtorExternalApiGateway` instance
- **Parameter**: `externalApiGateway` injected into `Application.module()` 
- **Defaults**: Resolved from `appConfig.externalApi`

#### 6. Test Coverage
- **`ExternalApiGatewayTest.kt`** (5 tests):
  - `getStatusReturnsHealthData()` — verifies endpoint URL + response mapping
  - `getTeamReturnsTeamData()` — single resource fetch with ID param
  - `getFixturesReturnsFixturesWithQueryParams()` — list endpoint with season/limit params
  - `getStatusWithApiKeyHeaderIncludesAuth()` — validates API key injection
  - `getStatusThrowsExternalApiExceptionOn500Error()` — error mapping

- **`ExternalApiConfigTest.kt`** (6 tests):
  - Default config is disabled
  - Custom config with all fields
  - Validation: required base URL when enabled
  - Validation: HTTPS/HTTP protocol enforcement
  - Validation: positive timeout values  
  - Allows disabled with empty URL

- **`TestAuthSupport.kt`**: Updated `createTestAppConfig()` to include disabled `externalApi` by default

#### 7. Architecture Decisions (Finalized)
1. **Error handling**: `ExternalApiException` thrown on all failures (transport, timeout, 5xx responses). Callers can catch and decide: return null, throw domain exception, or retry.
2. **Package placement**: New `integration/` package for adapter; applies same pattern as existing `persistence/` package.
3. **Auth strategy**: Pluggable header name + optional key (first-cut bearer support; easy to extend to OAuth, JWT).
4. **Disabled behavior**: Gateway returns `DisabledExternalApiGateway` by default; safe for dev/test without external dependencies.

### Usage Example
```kotlin
// In an application service or API route:
suspend fun someHandler(gateway: ExternalApiGateway) {
    val status = gateway.getStatus()  // Throws ExternalApiException on failure
    val team = gateway.getTeam("THW-KIEL")
    val fixtures = gateway.getFixtures(season = "2025", limit = 10)
}

// Via environment (first startup):
export EXTERNAL_API_ENABLED=true
export EXTERNAL_API_BASE_URL=https://api.handball-fed.de
export EXTERNAL_API_KEY=secret123
./gradlew :server:run

// Via .env file:
EXTERNAL_API_ENABLED=true
EXTERNAL_API_BASE_URL=https://api.handball-fed.de
EXTERNAL_API_KEY=secret123
```

### Files Modified/Created
- ✓ `gradle/libs.versions.toml` — added Ktor client aliases
- ✓ `server/build.gradle.kts` — wired client dependencies  
- ✓ `server/src/main/kotlin/de/exhumedo/kmp/handball_support/application/ExternalApiGateway.kt` (new)
- ✓ `server/src/main/kotlin/de/exhumedo/kmp/handball_support/integration/KtorExternalApiGateway.kt` (new)
- ✓ `server/src/main/kotlin/de/exhumedo/kmp/handball_support/config/AppConfig.kt` — extended with `ExternalApiConfig`
- ✓ `server/src/main/kotlin/de/exhumedo/kmp/handball_support/Application.kt` — added wiring + client factory
- ✓ `server/src/test/kotlin/de/exhumedo/kmp/handball_support/ExternalApiGatewayTest.kt` (new)
- ✓ `server/src/test/kotlin/de/exhumedo/kmp/handball_support/ExternalApiConfigTest.kt` (new)
- ✓ `server/src/test/kotlin/de/exhumedo/kmp/handball_support/TestAuthSupport.kt` — updated fixture

### Next Steps (Optional Enhancements)
- [ ] Inject gateway into a real application service for domain logic
- [ ] Add circuit breaker / retry policy (e.g., with `kotlinx-resilience`)
- [ ] Support additional HTTP methods (POST, PUT, DELETE)
- [ ] Add OpenMetrics instrumentation for latency/error rates
- [ ] Document API contract in `docs/openapi.yaml` for internal integration endpoints

