# Sportradar FMP Handball Datafeed API Integration

## Overview

This server module now integrates with the **Sportradar FMP Handball Datafeed API** to fetch real-time tournament data, standings, fixtures, and team information. The integration follows your hexagonal architecture with a clean port-adapter pattern.

**PDF Reference**: `20180904-Sportradar_FMP Handball Datafeed API.pdf`

---

## Key Components

### 1. Port Interface: `ExternalApiGateway`
**Location**: `application/ExternalApiGateway.kt`

Defines four Sportradar-specific methods:

```kotlin
interface ExternalApiGateway {
    suspend fun getTournament(): SportradarTournamentFeedDto
    suspend fun getStandings(phaseId: String): SportradarStandingsFeedDto
    suspend fun getFixtures(tournamentId: String, seasonId: String): SportradarFixturesFeedDto
    suspend fun getTeamInfo(tournamentId: String, seasonId: String, teamId: String): SportradarTeamInfoFeedDto
}
```

Each method returns a feed DTO containing:
- `meta`: Feed metadata (_dob, _maxage)
- `payload`: Raw JSON response from Sportradar

### 2. Adapter: `KtorExternalApiGateway`
**Location**: `integration/KtorExternalApiGateway.kt`

Implements the port with:
- **URL construction**: `https://hbl.fmp.sportradar.com/feeds/{language}/{timeZone}/{product}/...`
- **Query params**: Supports optional API key as query parameter
- **Headers**: Falls back to API key header if not using query params
- **Error handling**: Maps transport errors to `ExternalApiException`
- **JSON parsing**: Extracts metadata (_dob, _maxage) from feed envelope

### 3. Configuration: `ExternalApiConfig`
**Location**: `config/AppConfig.kt`

Supports full Sportradar endpoint customization:

```kotlin
data class ExternalApiConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "https://hbl.fmp.sportradar.com",
    val language: String = "en",           // Feed language
    val timeZone: String = "Europe:Berlin",// Timezone for dates
    val product: String = "gismo",         // Product ID
    val apiKey: String? = null,
    val apiKeyQueryParamName: String = "api_key",
    val sendApiKeyAsQueryParam: Boolean = false,
    val apiKeyHeaderName: String = "X-API-Key",
    val requestTimeoutMillis: Long = 5_000,
    val connectTimeoutMillis: Long = 3_000,
)
```

### 4. Example Routes
**Location**: `api/example/ExternalApiExampleRoutes.kt`

Four example routes proxy to Sportradar:

```
GET  /api/sportradar/tournament
     → Fetch all non-archived tournaments

GET  /api/sportradar/standings/{phaseId}
     → Fetch standings for a tournament phase

GET  /api/sportradar/fixtures/{tournamentId}/{seasonId}
     → Fetch all fixtures for a tournament season

GET  /api/sportradar/team-info/{tournamentId}/{seasonId}/{teamId}
     → Fetch detailed team information
```

---

## Sportradar API Routes

Based on the PDF specification, the adapter constructs these Sportradar URLs:

| Endpoint | Purpose |
|---|---|
| `/feeds/{lang}/{tz}/gismo/tournament/` | All tournaments |
| `/feeds/{lang}/{tz}/gismo/standings/{phase_id}` | Standings by phase |
| `/feeds/{lang}/{tz}/gismo/fixtures/{tournament_id}/{season_id}` | Fixtures by tournament & season |
| `/feeds/{lang}/{tz}/gismo/team_info/{tournament_id}/{season_id}/{team_id}` | Team details |

**Example URL**:
```
https://hbl.fmp.sportradar.com/feeds/en/Europe:Berlin/gismo/tournament/
https://hbl.fmp.sportradar.com/feeds/en/Europe:Berlin/gismo/standings/PHASE-001
https://hbl.fmp.sportradar.com/feeds/en/Europe:Berlin/gismo/fixtures/921/33765?api_key=YOUR_KEY
```

---

## Sportradar Data Structures

### Tournament Feed
```json
{
  "Doc": {
    "_dob": "2026-04-15T10:30:00Z",
    "_maxage": 300000,
    "Data": {
      "Tournaments": [
        {
          "_id": "...",
          "_sid": "...",
          "name": "...",
          "seasons": [...]
        }
      ]
    }
  }
}
```

### Standings Feed
```json
{
  "Doc": {
    "_dob": "2026-04-15T10:30:00Z",
    "_maxage": 300000,
    "Data": {
      "Tournament": {...},
      "Season": {...},
      "Phase": {...},
      "Standings_main": [...],
      "Standings_home": [...],
      "Standings_guest": [...]
    }
  }
}
```

**Standings entry**:
- _id, _sid, name, abbreviation
- position, played_games, total_games
- wins, draws, losses
- points_pro, points_against, goal_difference
- goals_pro, goals_against

### Fixtures Feed
```json
{
  "Doc": {
    "_dob": "2026-04-15T10:30:00Z",
    "_maxage": 600000,
    "Data": {
      "Tournament": {...},
      "Matchdays": [
        {
          "_id": "...",
          "title": "...",
          "Matches": [
            {
              "_id": "...",
              "status": "scheduled",
              "Location": "...",
              "Play_date": "...",
              "Home_team": {...},
              "Away_team": {...},
              "livescore": {...}
            }
          ]
        }
      ]
    }
  }
}
```

### Team Info Feed
```json
{
  "Doc": {
    "_dob": "2026-04-15T10:30:00Z",
    "_maxage": 300000,
    "Data": {
      "Tournament": {...},
      "Season": {...},
      "Team_info": {
        "_id": "...",
        "name": "...",
        "abbreviation": "...",
        "Url": {},
        "Contact": {},
        "Squad": [...],
        "Staff": [...]
      }
    }
  }
}
```

---

## Setup & Configuration

### 1. Enable in .env
```bash
EXTERNAL_API_ENABLED=true
EXTERNAL_API_BASE_URL=https://hbl.fmp.sportradar.com
EXTERNAL_API_LANGUAGE=en
EXTERNAL_API_TIME_ZONE=Europe:Berlin
EXTERNAL_API_PRODUCT=gismo
EXTERNAL_API_KEY=your-sportradar-api-key
EXTERNAL_API_KEY_AS_QUERY_PARAM=true
EXTERNAL_API_KEY_QUERY_PARAM=api_key
EXTERNAL_API_REQUEST_TIMEOUT_MS=10000
EXTERNAL_API_CONNECT_TIMEOUT_MS=5000
```

### 2. Or via environment variables
```bash
export EXTERNAL_API_ENABLED=true
export EXTERNAL_API_KEY=your-key
./gradlew :server:run
```

### 3. Or via system properties
```bash
./gradlew :server:run \
  -Dexternal.api.enabled=true \
  -Dexternal.api.key=your-key
```

---

## Usage Example

### In an Application Service
```kotlin
class HandballAnalysisService(
    private val sportradarGateway: ExternalApiGateway
) {
    suspend fun analyzeLeague(tournamentId: String, seasonId: String) {
        try {
            val fixtures = sportradarGateway.getFixtures(tournamentId, seasonId)
            val metadata = fixtures.meta
            val payload = fixtures.payload
            
            // Parse and process Sportradar JSON payload
            println("Feed generated at: ${metadata.generatedAt}")
            println("Cache for: ${metadata.minCacheMillis} ms")
        } catch (e: ExternalApiException) {
            logger.error("Failed to fetch Sportradar data: ${e.message}")
        }
    }
}
```

### Via API Route
```bash
curl http://localhost:8080/api/sportradar/standings/PHASE-001
curl http://localhost:8080/api/sportradar/fixtures/921/33765
curl http://localhost:8080/api/sportradar/team-info/921/33765/TEAM-001
```

---

## Error Handling

All gateway methods throw `ExternalApiException` for:
- Network timeouts
- HTTP 4xx/5xx responses
- Connection failures
- Serialization errors

Example:
```kotlin
try {
    val standings = externalGateway.getStandings(phaseId)
} catch (e: ExternalApiException) {
    call.respond(
        HttpStatusCode.BadGateway,
        mapOf("error" to e.message)
    )
}
```

---

## Tests

Run Sportradar-specific tests:
```bash
./gradlew :server:test --tests "SportradarExternalApiGatewayTest"
```

Test coverage:
- ✅ `getTournamentReturnsValidFeed()` — Tournament endpoint
- ✅ `getStandingsReturnsValidFeed()` — Standings endpoint
- ✅ `getFixturesReturnsValidFeed()` — Fixtures endpoint
- ✅ `getTeamInfoReturnsValidFeed()` — Team info endpoint
- ✅ `sendsApiKeyAsQueryParameterWhenConfigured()` — Query param auth
- ✅ Config validation tests (baseUrl, language, product, etc.)

---

## Next Steps

1. **Parse feeds**: Add domain mappers to convert Sportradar JSON to domain models
2. **Cache**: Implement caching using the `_maxage` field from feed metadata
3. **Scheduled sync**: Add periodic jobs to pre-fetch and cache Sportradar data
4. **WebSocket**: Stream live scores via WebSocket to frontend
5. **Monitoring**: Track API call latencies and error rates

---

## Reference

- **PDF**: `20180904-Sportradar_FMP Handball Datafeed API.pdf`
- **Base URL**: `https://hbl.fmp.sportradar.com`
- **Port**: `ExternalApiGateway` in `application/ExternalApiGateway.kt`
- **Adapter**: `KtorExternalApiGateway` in `integration/KtorExternalApiGateway.kt`
- **Config**: `ExternalApiConfig` in `config/AppConfig.kt`
- **Examples**: `configureSportradarApiRoutes()` in `api/example/ExternalApiExampleRoutes.kt`
- **Tests**: `SportradarExternalApiGatewayTest.kt`
